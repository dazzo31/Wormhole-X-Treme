package com.wormhole_xtreme.wormhole.permissions;

import com.wormhole_xtreme.wormhole.WormholeXTreme;
import org.bukkit.entity.Player;

/**
 * Utility class for permission-related operations.
 */
public class PermissionUtils {
    
    /**
     * Checks if a player has the specified permission.
     *
     * @param player The player to check
     * @param permission The permission node to check
     * @return true if the player has the permission, false otherwise
     */
    public static boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        // Check if the player has the permission directly
        if (player.hasPermission(permission)) {
            return true;
        }
        
        // Check if the player is an op (bypasses all permissions)
        if (player.isOp()) {
            return true;
        }
        
        // Check if the player has the wildcard permission
        String[] parts = permission.split("\\.");
        StringBuilder permBuilder = new StringBuilder();
        
        for (int i = 0; i < parts.length - 1; i++) {
            permBuilder.append(parts[i]).append(".");
            if (player.hasPermission(permBuilder + "*")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the player's permission level based on the configuration.
     *
     * @param player The player to check
     * @return The player's permission level
     */
    public static PermissionLevel getPermissionLevel(Player player) {
        if (player == null) {
            return PermissionLevel.WORMHOLE_NO_PERMISSIONS;
        }
        
        // Check for admin permission
        if (hasPermission(player, "wormhole.admin") || player.isOp()) {
            return PermissionLevel.WORMHOLE_ADMIN_PERMISSION;
        }
        
        // Check for config permission
        if (hasPermission(player, "wormhole.config")) {
            return PermissionLevel.WORMHOLE_CONFIG_PERMISSION;
        }
        
        // Check for build permission
        if (hasPermission(player, "wormhole.build")) {
            return PermissionLevel.WORMHOLE_BUILD_PERMISSION;
        }
        
        // Check for use permission
        if (hasPermission(player, "wormhole.use")) {
            return PermissionLevel.WORMHOLE_USE_PERMISSION;
        }
        
        // Check for simple permissions if enabled
        if (WormholeXTreme.getInstance().getWormholeConfig().get("permissions.simple_mode")) {
            if (hasPermission(player, "wormhole.simple.admin")) {
                return PermissionLevel.WORMHOLE_ADMIN_PERMISSION;
            }
            if (hasPermission(player, "wormhole.simple.config")) {
                return PermissionLevel.WORMHOLE_CONFIG_PERMISSION;
            }
            if (hasPermission(player, "wormhole.simple.build")) {
                return PermissionLevel.WORMHOLE_BUILD_PERMISSION;
            }
            if (hasPermission(player, "wormhole.simple.use")) {
                return PermissionLevel.WORMHOLE_USE_PERMISSION;
            }
        }
        
        // Return default permission level from config
        try {
            return WormholeXTreme.getInstance().getWormholeConfig().getDefaultPermissionLevel();
        } catch (IllegalArgumentException e) {
            return PermissionLevel.WORMHOLE_NO_PERMISSIONS;
        }
    }
    
    /**
     * Checks if a player can build stargates based on the build restriction settings.
     *
     * @param player The player to check
     * @param gatesOwned The number of gates the player currently owns
     * @return true if the player can build more gates, false otherwise
     */
    public static boolean canBuildMoreGates(Player player, int gatesOwned) {
        if (player == null) {
            return false;
        }
        
        // Admins can always build gates
        if (getPermissionLevel(player).isAtLeast(PermissionLevel.WORMHOLE_ADMIN_PERMISSION)) {
            return true;
        }
        
        // Check build restrictions if enabled
        WormholeConfig config = WormholeXTreme.getInstance().getWormholeConfig();
        if (!config.get("build_restriction.enabled")) {
            return true;
        }
        
        // Check which build group the player is in
        int maxGates = 0;
        if (hasPermission(player, "wormhole.build.group3")) {
            maxGates = config.get("build_restriction.group_three");
        } else if (hasPermission(player, "wormhole.build.group2")) {
            maxGates = config.get("build_restriction.group_two");
        } else if (hasPermission(player, "wormhole.build.group1")) {
            maxGates = config.get("build_restriction.group_one");
        }
        
        return gatesOwned < maxGates;
    }
    
    /**
     * Gets the cooldown time in seconds for a player based on their permission group.
     *
     * @param player The player to check
     * @return The cooldown time in seconds, or 0 if no cooldown
     */
    public static int getCooldownTime(Player player) {
        if (player == null) {
            return 0;
        }
        
        // Check if cooldowns are enabled
        WormholeConfig config = WormholeXTreme.getInstance().getWormholeConfig();
        if (!config.get("cooldown.enabled")) {
            return 0;
        }
        
        // Check which cooldown group the player is in
        if (hasPermission(player, "wormhole.cooldown.bypass")) {
            return 0;
        } else if (hasPermission(player, "wormhole.cooldown.group3")) {
            return config.get("cooldown.group_three");
        } else if (hasPermission(player, "wormhole.cooldown.group2")) {
            return config.get("cooldown.group_two");
        } else if (hasPermission(player, "wormhole.cooldown.group1")) {
            return config.get("cooldown.group_one");
        }
        
        return 0;
    }
}
