package com.wormhole_xtreme.wormhole.permissions;

import com.wormhole_xtreme.wormhole.WormholeXTreme;
import com.wormhole_xtreme.wormhole.config.WormholeConfig;
import org.bukkit.entity.Player;

/**
 * Utility class for permission-related operations.
 */
public class PermissionUtils {

    public static boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null) return false;

        if (player.hasPermission(permission)) return true;
        if (player.isOp()) return true;

        // wildcard support
        String[] parts = permission.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            sb.append(parts[i]).append('.');
            if (player.hasPermission(sb + "*")) return true;
        }
        return false;
    }

    public static PermissionsManager.PermissionLevel getPermissionLevel(Player player) {
        if (player == null) return PermissionsManager.PermissionLevel.WORMHOLE_NO_PERMISSION;

        if (hasPermission(player, "wormhole.admin") || player.isOp()) {
            return PermissionsManager.PermissionLevel.WORMHOLE_FULL_PERMISSION;
        }
        if (hasPermission(player, "wormhole.create") || hasPermission(player, "wormhole.build")) {
            return PermissionsManager.PermissionLevel.WORMHOLE_CREATE_PERMISSION;
        }
        if (hasPermission(player, "wormhole.use")) {
            return PermissionsManager.PermissionLevel.WORMHOLE_USE_PERMISSION;
        }

        // simple permissions
        if (WormholeXTreme.getInstance().getWormholeConfig().get(WormholeConfig.SIMPLE_PERMISSIONS)) {
            if (hasPermission(player, "wormhole.simple.use")) {
                return PermissionsManager.PermissionLevel.WORMHOLE_USE_PERMISSION;
            }
            if (hasPermission(player, "wormhole.simple.build")) {
                return PermissionsManager.PermissionLevel.WORMHOLE_CREATE_PERMISSION;
            }
        }

        try {
            return WormholeXTreme.getInstance().getWormholeConfig().getDefaultPermissionLevel();
        } catch (Exception e) {
            return PermissionsManager.PermissionLevel.WORMHOLE_NO_PERMISSION;
        }
    }

    public static boolean canBuildMoreGates(Player player, int gatesOwned) {
        if (player == null) return false;

        // ops/admin bypass
        if (player.isOp() || getPermissionLevel(player) == PermissionsManager.PermissionLevel.WORMHOLE_FULL_PERMISSION) {
            return true;
        }

        WormholeConfig config = WormholeXTreme.getInstance().getWormholeConfig();
        if (!config.get(WormholeConfig.BUILD_RESTRICTION_ENABLED)) return true;

        int maxGates = 0;
        if (hasPermission(player, "wormhole.build.group3")) {
            maxGates = config.get(WormholeConfig.BUILD_RESTRICTION_GROUP_THREE);
        } else if (hasPermission(player, "wormhole.build.group2")) {
            maxGates = config.get(WormholeConfig.BUILD_RESTRICTION_GROUP_TWO);
        } else if (hasPermission(player, "wormhole.build.group1")) {
            maxGates = config.get(WormholeConfig.BUILD_RESTRICTION_GROUP_ONE);
        }
        return gatesOwned < maxGates;
    }

    public static int getCooldownTime(Player player) {
        if (player == null) return 0;

        WormholeConfig config = WormholeXTreme.getInstance().getWormholeConfig();
        if (!config.get(WormholeConfig.USE_COOLDOWN_ENABLED)) return 0;

        if (hasPermission(player, "wormhole.cooldown.bypass")) return 0;
        if (hasPermission(player, "wormhole.cooldown.group3")) return config.get(WormholeConfig.USE_COOLDOWN_GROUP_THREE);
        if (hasPermission(player, "wormhole.cooldown.group2")) return config.get(WormholeConfig.USE_COOLDOWN_GROUP_TWO);
        if (hasPermission(player, "wormhole.cooldown.group1")) return config.get(WormholeConfig.USE_COOLDOWN_GROUP_ONE);
        return 0;
    }
}
