/*
 *   Wormhole X-Treme Plugin for Bukkit
 *   Copyright (C) 2011  Ben Echols
 *                       Dean Bailey
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wormhole_xtreme.wormhole;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.wormhole_xtreme.wormhole.config.WormholeConfig;
import com.wormhole_xtreme.wormhole.logic.StargateHelper;
import com.wormhole_xtreme.wormhole.model.Stargate;
import com.wormhole_xtreme.wormhole.model.StargateManager;
import com.wormhole_xtreme.wormhole.model.StargateShape;
import com.wormhole_xtreme.wormhole.permissions.StargateRestrictions;
import com.wormhole_xtreme.wormhole.permissions.WXPermissions;
import com.wormhole_xtreme.wormhole.permissions.WXPermissions.PermissionType;
import com.wormhole_xtreme.wormhole.utils.WorldUtils;

/**
 * The Class WormholeXTremePlayerListener.
 */

/**
 * WormholeXtreme Player Listener.
 * 
 * @author Ben Echols (Lologarithm)
 * @author Dean Bailey (alron)
 */
public class WormholeXTremePlayerListener implements Listener {
    
    private final JavaPlugin plugin;
    
    /**
     * Instantiates a new wormhole xtreme player listener.
     *
     * @param plugin the plugin
     */
    public WormholeXTremePlayerListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Button lever hit.
     * 
     * @param player
     *            the p
     * @param clickedBlock
     *            the clicked
     * @param direction
     *            the direction
     * @return true, if successful
     */
    private static boolean buttonLeverHit(final Player player, final Block clickedBlock, BlockFace direction)
    {
        final Stargate stargate = StargateManager.getGateFromBlock(clickedBlock);

        if (stargate != null)
        {
            if (WorldUtils.isSameBlock(stargate.getGateDialLeverBlock(), clickedBlock) && ((stargate.isGateSignPowered() && WXPermissions.checkWXPermissions(player, stargate, PermissionType.SIGN)) || ( !stargate.isGateSignPowered() && WXPermissions.checkWXPermissions(player, stargate, PermissionType.DIALER))))
            {
                handleGateActivationSwitch(stargate, player);
            }
            else if (WorldUtils.isSameBlock(stargate.getGateIrisLeverBlock(), clickedBlock) && ( !stargate.isGateSignPowered() && WXPermissions.checkWXPermissions(player, stargate, PermissionType.DIALER)))
            {
                stargate.toggleIrisActive(true);
            }
            else if (WorldUtils.isSameBlock(stargate.getGateIrisLeverBlock(), clickedBlock) || WorldUtils.isSameBlock(stargate.getGateDialLeverBlock(), clickedBlock))
            {
                player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_PERMISSION_DENIED));
            }
            return true;
        }
        else
        {
            if (direction == null)
            {
                switch (clickedBlock.getData())
                {
                    case 1 :
                        direction = BlockFace.SOUTH;
                        break;
                    case 2 :
                        direction = BlockFace.NORTH;
                        break;
                    case 3 :
                        direction = BlockFace.WEST;
                        break;
                    case 4 :
                        direction = BlockFace.EAST;
                        break;
                    default :
                        break;
                }

                if (direction == null)
                {
                    return false;
                }
            }
            // Check to see if player has already run the "build" command.
            final StargateShape shape = StargateManager.getPlayerBuilderShape(player);

            Stargate newGate = null;
            if (shape != null)
            {
                newGate = StargateHelper.checkStargate(clickedBlock, direction, shape);
            }
            else
            {
                WormholeXTreme.getThisPlugin().prettyLog(Level.FINEST, false, "Attempting to find any gate shapes!");
                newGate = StargateHelper.checkStargate(clickedBlock, direction);
            }

            if (newGate != null)
            {
                if (WXPermissions.checkWXPermissions(player, newGate, PermissionType.BUILD) && !StargateRestrictions.isPlayerBuildRestricted(player))
                {
                    if (newGate.isGateSignPowered())
                    {
                        player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_NORMAL_HEADER) + "Stargate Design Valid with Sign Nav.");
                        if (newGate.getGateName().equals(""))
                        {
                            player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_CONSTRUCT_NAME_INVALID) + "\"\"");
                        }
                        else
                        {
                            final boolean success = StargateManager.completeStargate(player, newGate);
                            if (success)
                            {
                                player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_CONSTRUCT_SUCCESS));
                                newGate.getGateDialSign().setLine(0, "-" + newGate.getGateName() + "-");
                                newGate.getGateDialSign().setData(newGate.getGateDialSign().getData());
                                newGate.getGateDialSign().update();
                            }
                            else
                            {
                                player.sendMessage("Stargate constrution failed!?");
                            }
                        }

                    }
                    else
                    {
                        // Print to player that it was successful!
                        player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_NORMAL_HEADER) + "Valid Stargate Design! \u00A73:: \u00A7B<required> \u00A76[optional]");
                        player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_NORMAL_HEADER) + "Type \'\u00A7F/wxcomplete \u00A7B<name> \u00A76[idc=IDC] [net=NET]\u00A77\' to complete.");
                        // Add gate to unnamed gates.
                        StargateManager.addIncompleteStargate(player, newGate);
                    }
                    return true;
                }
                else
                {
                    if (newGate.isGateSignPowered())
                    {
                        newGate.resetTeleportSign();
                    }
                    StargateManager.removeIncompleteStargate(player);
                    if (StargateRestrictions.isPlayerBuildRestricted(player))
                    {
                        player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_BUILD_RESTRICTED));
                    }
                    player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_PERMISSION_DENIED));
                    return true;
                }
            }
            else
            {
                WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, player.getName() + " has pressed a button or lever but did not find any properly created gates.");
            }
        }
        return false;
    }

    /**
     * Handle gate activation switch.
     * 
     * @param stargate
     *            the stargate
     * @param player
     *            the player
     */
    private static void handleGateActivationSwitch(Stargate stargate, Player player) {
        if (stargate.isGateActive() || stargate.isGateLightsActive())
        {
            if (stargate.getGateTarget() != null)
            {
                //Shutdown stargate
                stargate.shutdownStargate(true);
                player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_GATE_SHUTDOWN));
            }
            else
            {
                final Stargate s2 = StargateManager.removeActivatedStargate(player);
                if ((s2 != null) && (stargate.getGateId() == s2.getGateId()))
                {
                    stargate.stopActivationTimer();
                    stargate.setGateActive(false);
                    stargate.toggleDialLeverState(false);
                    stargate.lightStargate(false);
                    player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_GATE_DEACTIVATED));
                }
                else
                {
                    if (stargate.isGateLightsActive() && !stargate.isGateActive())
                    {
                        player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_ERROR_HEADER) + "Gate has been activated by someone else already.");
                    }
                    else
                    {
                        player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_GATE_REMOVE_ACTIVE));
                    }
                }
            }
        }
        else
        {
            if (stargate.isGateSignPowered())
            {
                if (WXPermissions.checkWXPermissions(player, stargate, PermissionType.SIGN))
                {
                    if ((stargate.getGateDialSign() == null) && (stargate.getGateDialSignBlock() != null))
                    {
                        stargate.tryClickTeleportSign(stargate.getGateDialSignBlock());
                    }

                    if (stargate.getGateDialSignTarget() != null)
                    {
                        if (stargate.dialStargate(stargate.getGateDialSignTarget(), false))
                        {
                            player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_NORMAL_HEADER) + "Stargates connected!");
                        }
                        else
                        {
                            player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_GATE_REMOVE_ACTIVE));
                        }
                    }
                    else
                    {
                        player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_TARGET_INVALID));
                    }
                }
                else
                {
                    player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_PERMISSION_DENIED));
                }
            }
            else
            {
                //Activate Stargate
                player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_GATE_ACTIVATED));
                player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_NORMAL_HEADER) + "Chevrons Locked! \u00A73:: \u00A7B<required> \u00A76[optional]");
                player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_NORMAL_HEADER) + "Type \'\u00A7F/dial \u00A7B<gatename> \u00A76[idc]\u00A77\'");
                StargateManager.addActivatedStargate(player, stargate);
                stargate.startActivationTimer(player);
                stargate.lightStargate(true);
            }
        }
    }

    /**
     * Handle player interact event.
     * 
     * @param event
     *            the event
     * @return true, if successful
     */
    /**
     * Handle player interact event.
     *
     * @param event the PlayerInteractEvent
     * @return true if the event should be cancelled
     */
    private static boolean handlePlayerInteractEvent(final PlayerInteractEvent event) {
        final Block clickedBlock = event.getClickedBlock();
        final Player player = event.getPlayer();

        if (clickedBlock == null) {
            return false;
        }

        // Handle button/lever interactions
        if (clickedBlock.getType() == Material.LEVER || clickedBlock.getType() == Material.STONE_BUTTON || 
            clickedBlock.getType() == Material.WOOD_BUTTON) {
            return buttonLeverHit(player, clickedBlock, null);
        } 
        // Handle sign interactions
        else if (clickedBlock.getType() == Material.WALL_SIGN || clickedBlock.getType() == Material.SIGN_POST) {
            final Stargate stargate = StargateManager.getGateFromBlock(clickedBlock);
            if (stargate != null) {
                if (WXPermissions.checkWXPermissions(player, stargate, PermissionType.SIGN)) {
                    return stargate.tryClickTeleportSign(clickedBlock, player);
                } else {
                    player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_PERMISSION_DENIED));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the block coordinates have changed between two locations.
     *
     * @param fromLoc the starting location
     * @param toLoc the ending location
     * @return true if the block coordinates have changed, false otherwise
     */
    private static boolean hasChangedBlockCoordinates(final Location fromLoc, final Location toLoc) {
        return fromLoc.getBlockX() != toLoc.getBlockX() ||
               fromLoc.getBlockY() != toLoc.getBlockY() ||
               fromLoc.getBlockZ() != toLoc.getBlockZ();
    }

    /**
     * Handle player move event.
     * 
     * @param event
     *            the event
     * @return true, if successful
     */
    private static boolean handlePlayerMoveEvent(final PlayerMoveEvent event)
    {
        if (!hasChangedBlockCoordinates(event.getFrom(), event.getTo())) 
        {
            return false;
        }
        final Player player = event.getPlayer();
        final Location toLocFinal = event.getTo();
        final Block gateBlockFinal = toLocFinal.getWorld().getBlockAt(toLocFinal.getBlockX(), toLocFinal.getBlockY(), toLocFinal.getBlockZ());
        final Stargate stargate = StargateManager.getGateFromBlock(gateBlockFinal);

        if ((stargate != null) && stargate.isGateActive() && (stargate.getGateTarget() != null) && (gateBlockFinal.getTypeId() == (stargate.isGateCustom()
            ? stargate.getGateCustomPortalMaterial().getId()
            : stargate.getGateShape() != null
                ? stargate.getGateShape().getShapePortalMaterial().getId()
                : Material.STATIONARY_WATER.getId())))
        {
            String gatenetwork;
            if (stargate.getGateNetwork() != null)
            {
                gatenetwork = stargate.getGateNetwork().getNetworkName();
            }
            else
            {
                gatenetwork = "Public";
            }
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Player in gate:" + stargate.getGateName() + " gate Active: " + stargate.isGateActive() + " Target Gate: " + stargate.getGateTarget().getGateName() + " Network: " + gatenetwork);

            if (WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.WORMHOLE_USE_IS_TELEPORT) && ((stargate.isGateSignPowered() && !WXPermissions.checkWXPermissions(player, stargate, PermissionType.SIGN)) || ( !stargate.isGateSignPowered() && !WXPermissions.checkWXPermissions(player, stargate, PermissionType.DIALER))))
            {
                player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_PERMISSION_DENIED));
                return false;
            }

            if (WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.USE_COOLDOWN_ENABLED))
            {
                if (StargateRestrictions.isPlayerUseCooldown(player))
                {
                    player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_COOLDOWN_RESTRICTED));
                    player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_COOLDOWN_WAIT) + StargateRestrictions.checkPlayerUseCooldownRemaining(player));
                    return false;
                }
                else
                {
                    StargateRestrictions.addPlayerUseCooldown(player);
                }
            }

            if (stargate.getGateTarget().isGateIrisActive())
            {
                player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_ERROR_HEADER) + "Remote Iris is locked!");
                player.setNoDamageTicks(5);
                event.setFrom(stargate.getGatePlayerTeleportLocation());
                event.setTo(stargate.getGatePlayerTeleportLocation());
                player.teleport(stargate.getGatePlayerTeleportLocation());
                return true;
            }

            final Location target = stargate.getGateTarget().getGatePlayerTeleportLocation();
            player.setNoDamageTicks(5);
            event.setFrom(target);
            event.setTo(target);
            player.teleport(target);
            if (target != stargate.getGatePlayerTeleportLocation())
            {
                WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, player.getName() + " used wormhole: " + stargate.getGateName() + " to go to: " + stargate.getGateTarget().getGateName());
            }
            if (WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.TIMEOUT_SHUTDOWN) == 0)
            {
                stargate.shutdownStargate(true);
            }
            return true;
        }
        else if (stargate != null)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Player entered gate but wasn't active or didn't have a target.");
        }
        return false;
    }

    /**
     * Handle bucket event.
     *
     * @param player the player
     * @param block the block
     */
    private void handleBucketEvent(Player player, Block block) {
        Stargate stargate = StargateManager.getGateFromBlock(block);
        
        if (stargate != null) {
            // Cancel the event to prevent modifying the gate
            if (player != null) {
                player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_NORMAL_HEADER) + "You cannot place or remove blocks within a stargate.");
            }
        }
    }
    
    /**
     * On player bucket empty.
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        
        handleBucketEvent(player, block);
    }

    /**
     * On player bucket fill.
     *
     * @param event the event
    /**
     * On player interact.
     *
     * @param event the PlayerInteractEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Caught Player: \"" + event.getPlayer().getName() + 
                "\" Event type: \"" + event.getType() + "\" Action Type: \"" + event.getAction() + 
                "\" Event Block Type: \"" + event.getClickedBlock().getType() + 
                "\" Event World: \"" + event.getClickedBlock().getWorld().getName() + 
                "\" Event Block: " + event.getClickedBlock());
                
            if (handlePlayerInteractEvent(event)) {
                event.setCancelled(true);
                WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Cancelled Player: \"" + 
                    event.getPlayer().getName() + "\" Event type: \"" + event.getType() + 
                    "\" Action Type: \"" + event.getAction() + "\" Event Block Type: \"" + 
                    event.getClickedBlock().getType() + "\" Event World: \"" + 
                    event.getClickedBlock().getWorld().getName() + "\" Event Block: " + 
                    event.getClickedBlock());
            }
        } else {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Caught and ignored Player: \"" + 
                event.getPlayer().getName() + "\" Event type: \"" + event.getType() + "\"");
        }
    }

    /**
     * On player move.
     *
     * @param event the PlayerMoveEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (handlePlayerMoveEvent(event)) {
            event.setCancelled(true);
        }
    }
}
