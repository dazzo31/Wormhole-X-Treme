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
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.WallSign;
import java.util.logging.Level;

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
@SuppressWarnings("deprecation")
public class WormholeXTremePlayerListener implements Listener {

    /**
     * Handle lever/button interaction or attempt build if in builder mode.
     * Returns true if we handled and should cancel the event.
     */
    private static boolean buttonLeverHit(final Player player, final Block clickedBlock, final BlockFace direction) {
        final Stargate stargate = StargateManager.getGateFromBlock(clickedBlock);

        if (stargate != null) {
            final boolean dialLever = WorldUtils.isSameBlock(stargate.getGateDialLeverBlock(), clickedBlock);
            final boolean irisLever = WorldUtils.isSameBlock(stargate.getGateIrisLeverBlock(), clickedBlock);

            if (dialLever && ((stargate.isGateSignPowered() && WXPermissions.checkWXPermissions(player, stargate, PermissionType.SIGN))
                    || (!stargate.isGateSignPowered() && WXPermissions.checkWXPermissions(player, stargate, PermissionType.DIALER)))) {
                handleGateActivationSwitch(stargate, player);
                return true;
            } else if (irisLever && (!stargate.isGateSignPowered() && WXPermissions.checkWXPermissions(player, stargate, PermissionType.DIALER))) {
                stargate.toggleIrisActive(true);
                return true;
            } else if (dialLever || irisLever) {
                player.sendMessage("[WormholeXTreme] You do not have permission to perform this action.");
                return true;
            }
            return false;
        } else {
            // Legacy behavior tried to infer a new gate build here; we only proceed if a direction was provided.
            if (direction == null) {
                return false;
            }
            final StargateShape shape = StargateManager.getPlayerBuilderShape(player);
            final Stargate newGate = (shape != null)
                ? StargateHelper.checkStargate(clickedBlock, direction, shape)
                : StargateHelper.checkStargate(clickedBlock, direction);

            if (newGate != null) {
                if (WXPermissions.checkWXPermissions(player, newGate, PermissionType.BUILD) && !StargateRestrictions.isPlayerBuildRestricted(player)) {
                    if (newGate.isGateSignPowered()) {
                        player.sendMessage("[WormholeXTreme] Stargate design valid with sign nav.");
                        if (Objects.equals(newGate.getGateName(), "")) {
                            player.sendMessage("[WormholeXTreme] Invalid stargate name: \"\"");
                        } else {
                            final boolean success = StargateManager.completeStargate(player, newGate);
                            if (success) {
                                player.sendMessage("[WormholeXTreme] Stargate construction successful!");
                                final Sign sign = newGate.getGateDialSign();
                                if (sign != null) {
                                    sign.setLine(0, "-" + newGate.getGateName() + "-");
                                    sign.update();
                                }
                            } else {
                                player.sendMessage("[WormholeXTreme] Stargate construction failed!");
                            }
                        }
                    } else {
                        player.sendMessage("[WormholeXTreme] Valid Stargate Design! \u00A73:: \u00A7B<required> \u00A76[optional]");
                        player.sendMessage("[WormholeXTreme] Type '\u00A7F/wxcomplete \u00A7B<name> \u00A76[idc=IDC] [net=NET]\u00A77' to complete.");
                        StargateManager.addIncompleteStargate(player, newGate);
                    }
                    return true;
                } else {
                    if (newGate.isGateSignPowered()) {
                        newGate.resetTeleportSign();
                    }
                    StargateManager.removeIncompleteStargate(player);
                    if (StargateRestrictions.isPlayerBuildRestricted(player)) {
                        player.sendMessage("[WormholeXTreme] You are restricted from building Stargates.");
                    }
                    player.sendMessage("[WormholeXTreme] You do not have permission to perform this action.");
                    return true;
                }
            } else {
                WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, player.getName() +
                    " has pressed a button or lever but did not find any properly created gates.");
            }
            return false;
        }
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
                player.sendMessage("[WormholeXTreme] Stargate shutdown.");
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
                    player.sendMessage("[WormholeXTreme] Stargate deactivated.");
                }
                else
                {
                    if (stargate.isGateLightsActive() && !stargate.isGateActive())
                    {
                        player.sendMessage("[WormholeXTreme] Gate has been activated by someone else already.");
                    }
                    else
                    {
                        player.sendMessage("[WormholeXTreme] Gate is already active.");
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
                            player.sendMessage("[WormholeXTreme] Stargates connected!");
                        }
                        else
                        {
                            player.sendMessage("[WormholeXTreme] Gate is already active.");
                        }
                    }
                    else
                    {
                        player.sendMessage("[WormholeXTreme] Target gate is invalid.");
                    }
                }
                else
                {
                    player.sendMessage("[WormholeXTreme] You do not have permission to perform this action.");
                }
            }
            else
            {
                // Activate Stargate
                player.sendMessage("[WormholeXTreme] Stargate activated.");
                player.sendMessage("[WormholeXTreme] Chevrons Locked! \u00A73:: \u00A7B<required> \u00A76[optional]");
                player.sendMessage("[WormholeXTreme] Type '\u00A7F/dial \u00A7B<gatename> \u00A76[idc]\u00A77'");
                StargateManager.addActivatedStargate(player, stargate);
                stargate.startActivationTimer(player);
                stargate.lightStargate(true);
            }
        }
    }

    /**
     * Handle player interact event.
     * 
                player.sendMessage("[WormholeXTreme] Stargate activated.");
                player.sendMessage("[WormholeXTreme] Chevrons Locked! \u00A73:: \u00A7B<required> \u00A76[optional]");
                player.sendMessage("[WormholeXTreme] Type '\u00A7F/dial \u00A7B<gatename> \u00A76[idc]\u00A77'");
     */
    /**
     * Handle player interact event.
     *
     * @param event the PlayerInteractEvent
     * @return true if the event should be cancelled
     */
    private static boolean handlePlayerInteractEvent(final PlayerInteractEvent event) {
        final Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return false;
        }

        final Block clickedBlock = event.getClickedBlock();
        final Player player = event.getPlayer();
        if (clickedBlock == null) {
            return false;
        }

        // Lever or button
        final BlockData data = clickedBlock.getBlockData();
        if (data instanceof Switch) {
            return buttonLeverHit(player, clickedBlock, null);
        }

        // Wall sign interaction (cycle target)
        if (data instanceof WallSign) {
            final Stargate stargate = StargateManager.getGateFromBlock(clickedBlock);
            if (stargate != null) {
                if (!WXPermissions.checkWXPermissions(player, stargate, PermissionType.SIGN)) {
                    player.sendMessage("[WormholeXTreme] You do not have permission to perform this action.");
                    return true;
                }
                stargate.tryClickTeleportSign(clickedBlock);
                return true;
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

    if ((stargate != null) && stargate.isGateActive() && (stargate.getGateTarget() != null) && (gateBlockFinal.getType() == (stargate.isGateCustom()
            ? stargate.getGateCustomPortalMaterial()
            : stargate.getGateShape() != null
                ? stargate.getGateShape().getShapePortalMaterial()
                : Material.WATER)))
        {
            // Network name available via stargate.getGateNetwork(), kept for logging if needed
            // Debug: Player in gate: " + stargate.getGateName() + " gate Active: " + stargate.isGateActive() + " Target Gate: " + stargate.getGateTarget().getGateName() + " Network: " + gatenetwork;

            if (WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.WORMHOLE_USE_IS_TELEPORT) && ((stargate.isGateSignPowered() && !WXPermissions.checkWXPermissions(player, stargate, PermissionType.SIGN)) || ( !stargate.isGateSignPowered() && !WXPermissions.checkWXPermissions(player, stargate, PermissionType.DIALER))))
            {
                player.sendMessage("[WormholeXTreme] You do not have permission to perform this action.");
                return false;
            }

            if (WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.USE_COOLDOWN_ENABLED))
            {
                if (StargateRestrictions.isPlayerUseCooldown(player))
                {
                    player.sendMessage("[WormholeXTreme] You must wait before using another Stargate.");
                    player.sendMessage("[WormholeXTreme] Cooldown remaining: " + StargateRestrictions.checkPlayerUseCooldownRemaining(player));
                    return false;
                }
                else
                {
                    StargateRestrictions.addPlayerUseCooldown(player);
                }
            }

            if (stargate.getGateTarget().isGateIrisActive())
            {
                player.sendMessage("[WormholeXTreme] Remote Iris is locked!");
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
    private boolean handleBucketEvent(Player player, Block block) {
        Stargate stargate = StargateManager.getGateFromBlock(block);
        if (stargate != null) {
            if (player != null) {
                player.sendMessage("[WormholeXTreme] You cannot place or remove blocks within a stargate.");
            }
            return true;
        }
        return false;
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
        if (handleBucketEvent(player, block)) {
            event.setCancelled(true);
        }
    }

    /**
     * On player bucket fill.
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        if (handleBucketEvent(player, block)) {
            event.setCancelled(true);
        }
    }
    /**
     * On player interact.
     *
     * @param event the PlayerInteractEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            // Debug: Caught Player: ...
                
            if (handlePlayerInteractEvent(event)) {
                event.setCancelled(true);
                    WormholeXTreme.getThisPlugin().getLogger().fine("Cancelled Player: \"" + 
                        event.getPlayer().getName() + "\" Action Type: \"" + event.getAction() + 
                        "\" Event Block Type: \"" + event.getClickedBlock().getType() + 
                        "\" Event World: \"" + event.getClickedBlock().getWorld().getName() + 
                        "\" Event Block: " + event.getClickedBlock());
            }
        } else {
            WormholeXTreme.getThisPlugin().getLogger().fine("Caught and ignored Player: \"" + 
                event.getPlayer().getName() + "\"");
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
