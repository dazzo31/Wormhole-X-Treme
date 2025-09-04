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
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.wormhole_xtreme.wormhole.model.Stargate;
import com.wormhole_xtreme.wormhole.config.WormholeConfig;
import com.wormhole_xtreme.wormhole.permissions.WXPermissions;
import com.wormhole_xtreme.wormhole.permissions.WXPermissions.PermissionType;
import com.wormhole_xtreme.wormhole.utils.WorldUtils;

/**
 * The Class WormholeXTremeBlockListener.
 * Handles block-related events for WormholeXTreme.
 */

/**
 * WormholeXTreme Block Listener.
 * 
 * @author Ben Echols (Lologarithm)
 * @author Dean Bailey (alron)
 */
/**
 * WormholeXTreme Block Listener.
 * 
 * @author Ben Echols (Lologarithm)
 * @author Dean Bailey (alron)
 */
public class WormholeXTremeBlockListener implements Listener {
    
    private final JavaPlugin plugin;
    
    /**
     * Instantiates a new wormhole xtreme block listener.
     *
     * @param plugin the plugin
     */
    public WormholeXTremeBlockListener(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }
    /**
     * Handle block break.
     * 
     * @param player
     *            the player
     */
    private static boolean handleBlockBreak(final Player player, final Stargate stargate, final Block block) {
        if (player == null || stargate == null || block == null) {
            return false;
        }

        final boolean hasPermission = WXPermissions.checkWXPermissions(player, stargate, PermissionType.DAMAGE);
        if (!hasPermission) {
            player.sendMessage(WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.MESSAGE_PERMISSION_DENIED));
            return true;
        }

        // Handle dial lever block
        if (WorldUtils.isSameBlock(stargate.getGateDialLeverBlock(), block)) {
            return true; // Prevent breaking the dial lever
        }

        // Handle dial sign block
        if (stargate.getGateDialSignBlock() != null && WorldUtils.isSameBlock(stargate.getGateDialSignBlock(), block)) {
            player.sendMessage("Destroyed DHD Sign. You will be unable to change dialing target from this gate.");
            player.sendMessage("You can rebuild it later.");
            stargate.setGateDialSign(null);
            return false; // Allow breaking the sign
        }

        // Handle iris material
        Material irisMaterial = stargate.isGateCustom() 
            ? stargate.getGateCustomIrisMaterial() 
            : (stargate.getGateShape() != null ? stargate.getGateShape().getShapeIrisMaterial() : null);
            
        if (irisMaterial != null && block.getType() == irisMaterial) {
            return true; // Prevent breaking iris material
        }

        // Handle stargate blocks
        if (stargate.isGateActive()) {
            stargate.setGateActive(false);
            stargate.fillGateInterior(0);
        }
        
        if (stargate.isGateLightsActive()) {
            stargate.lightStargate(false);
            stargate.stopActivationTimer();
            StargateManager.removeActivatedStargate(player);
        }
        
        stargate.resetTeleportSign();
        stargate.setupGateSign(false);
        
        if (!stargate.getGateIrisDeactivationCode().isEmpty()) {
            stargate.setupIrisLever(false);
        }
        
        if (stargate.isGateRedstonePowered()) {
            stargate.setupRedstone(false);
        }
        StargateManager.removeStargate(stargate);
        player.sendMessage("Stargate Destroyed: " + stargate.getGateName());
        return true;
    }

    /**
     * On block break.
     *
     * @param event the BlockBreakEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Player player = event.getPlayer();
        final Stargate stargate = StargateManager.getGateFromBlock(block);

        if (stargate != null) {
            if (handleBlockBreak(player, stargate, block)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * On block burn.
     * Prevents blocks from burning near active stargates with lava portals.
     *
     * @param event the BlockBurnEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBurn(final BlockBurnEvent event) {
        final Location current = event.getBlock().getLocation();
        final Stargate closest = StargateManager.findClosestStargate(current);
        
        // Skip if no nearby stargate or stargate is inactive
        if (closest == null || !(closest.isGateActive() || closest.isGateRecentlyActive())) {
            return;
        }
        
        // Check if the portal uses lava
        Material portalMaterial = closest.isGateCustom() 
            ? closest.getGateCustomPortalMaterial()
            : (closest.getGateShape() != null 
                ? closest.getGateShape().getShapePortalMaterial() 
                : null);
                
        if (portalMaterial != Material.LAVA && portalMaterial != Material.STATIONARY_LAVA) {
            return;
        }
        
        // Check distance to stargate
        final double blockDistanceSquared = StargateManager.distanceSquaredToClosestGateBlock(current, closest);
        double wooshDepthSquared = closest.isGateCustom()
            ? closest.getGateCustomWooshDepthSquared()
            : (closest.getGateShape() != null 
                ? closest.getGateShape().getShapeWooshDepthSquared() 
                : 0);
                
        double wooshDepth = closest.isGateCustom()
            ? closest.getGateCustomWooshDepth()
            : (closest.getGateShape() != null 
                ? closest.getGateShape().getShapeWooshDepth() 
                : 0);
        
        // Cancel if within protected area
        if ((blockDistanceSquared <= wooshDepthSquared && wooshDepth != 0) || blockDistanceSquared <= 25) {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, 
                "Blocked Gate: \"" + closest.getGateName() + 
                "\" Proximity Block Burn Distance Squared: \"" + blockDistanceSquared + "\"");
            event.setCancelled(true);
        }
    }

    /**
     * On block damage.
     * Prevents players without permission from damaging stargate blocks.
     *
     * @param event the BlockDamageEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDamage(final BlockDamageEvent event) {
        final Stargate stargate = StargateManager.getGateFromBlock(event.getBlock());
        final Player player = event.getPlayer();
        
        if (stargate != null && player != null && 
            !WXPermissions.checkWXPermissions(player, stargate, PermissionType.DAMAGE)) {
            event.setCancelled(true);
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, 
                "Player: " + player.getName() + " denied damage on: " + stargate.getGateName());
        }
    }

    /**
     * On block from to.
     * Prevents blocks from flowing into stargate areas.
     *
     * @param event the BlockFromToEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockFromTo(final BlockFromToEvent event) {
        if (StargateManager.isBlockInGate(event.getToBlock()) || 
            StargateManager.isBlockInGate(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * On block ignite.
     * Prevents blocks from igniting near active stargates with lava portals.
     *
     * @param event the BlockIgniteEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockIgnite(final BlockIgniteEvent event) {
        final Location current = event.getBlock().getLocation();
        final Stargate closest = StargateManager.findClosestStargate(current);
        
        // Skip if no nearby stargate or stargate is inactive
        if (closest == null || !(closest.isGateActive() || closest.isGateRecentlyActive())) {
            return;
        }
        
        // Check if the portal uses lava
        Material portalMaterial = closest.isGateCustom() 
            ? closest.getGateCustomPortalMaterial()
            : (closest.getGateShape() != null 
                ? closest.getGateShape().getShapePortalMaterial() 
                : null);
                
        if (portalMaterial != Material.LAVA && portalMaterial != Material.STATIONARY_LAVA) {
            return;
        }
        
        // Check distance to stargate
        final double blockDistanceSquared = StargateManager.distanceSquaredToClosestGateBlock(current, closest);
        double wooshDepthSquared = closest.isGateCustom()
            ? closest.getGateCustomWooshDepthSquared()
            : (closest.getGateShape() != null 
                ? closest.getGateShape().getShapeWooshDepthSquared() 
                : 0);
                
        double wooshDepth = closest.isGateCustom()
            ? closest.getGateCustomWooshDepth()
            : (closest.getGateShape() != null 
                ? closest.getGateShape().getShapeWooshDepth() 
                : 0);
        
        // Cancel if within protected area
        if ((blockDistanceSquared <= wooshDepthSquared && wooshDepth != 0) || blockDistanceSquared <= 25) {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, 
                "Blocked Gate: \"" + closest.getGateName() + 
                "\" Block Type: \"" + event.getBlock().getType() + 
                "\" Proximity Block Ignite: \"" + event.getCause() + 
                "\" Distance Squared: \"" + blockDistanceSquared + "\"");
            event.setCancelled(true);
        }
    }

    /**
     * On block physics.
     * Prevents physics updates for blocks within stargates (except redstone).
     *
     * @param event the BlockPhysicsEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        final Block block = event.getBlock();
        if (StargateManager.isBlockInGate(block) && block.getType() != Material.REDSTONE_WIRE) {
            event.setCancelled(true);
        }
    }
}
