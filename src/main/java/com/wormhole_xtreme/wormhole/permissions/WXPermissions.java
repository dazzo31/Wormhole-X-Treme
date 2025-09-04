/**
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
package com.wormhole_xtreme.wormhole.permissions;

import org.bukkit.entity.Player;

import com.wormhole_xtreme.wormhole.WormholeXTreme;
import com.wormhole_xtreme.wormhole.config.WormholeConfig;
import com.wormhole_xtreme.wormhole.model.Stargate;
import com.wormhole_xtreme.wormhole.permissions.PermissionsManager.PermissionLevel;

/**
 * The Class WXPermissions.
 * 
 * @author alron
 */
public class WXPermissions
{

    /**
     * The Enum PermissionType.
     */
    public static enum PermissionType
    {

        /** The DAMAGE permission. */
        DAMAGE,

        /** The SIGN permission. */
        SIGN,

        /** The DIALER permission. */
        DIALER,

        /** The BUILD permission. */
        BUILD,

        /** The REMOVE permission. */
        REMOVE,

        /** The USE permission. */
        USE,

        /** The LIST permission. */
        LIST,

        /** The CONFIG permission. */
        CONFIG,

        /** The GO permission. */
        GO,

        /** The COMPASS permission. */
        COMPASS,

        USE_COOLDOWN_GROUP_ONE,

        USE_COOLDOWN_GROUP_TWO,

        USE_COOLDOWN_GROUP_THREE,

        BUILD_RESTRICTION_GROUP_ONE,
        BUILD_RESTRICTION_GROUP_TWO,
        BUILD_RESTRICTION_GROUP_THREE;
    }

    /**
     * Check if a player has a specific permission.
     * 
     * @param player the player to check
     * @param permissionType the type of permission to check
     * @return true if the player has the permission, false otherwise
     */
    public static boolean checkWXPermissions(final Player player, final PermissionType permissionType) {
        return checkWXPermissions(player, null, null, permissionType);
    }

    /**
     * Check if a player has a specific permission for a stargate.
     * 
     * @param player the player to check
     * @param stargate the stargate to check permissions for
     * @param permissionType the type of permission to check
     * @return true if the player has the permission, false otherwise
     */
    public static boolean checkWXPermissions(final Player player, final Stargate stargate, final PermissionType permissionType) {
        return checkWXPermissions(player, stargate, null, permissionType);
    }

    /**
     * Check if a player has a specific permission for a stargate and network.
     * 
     * @param player the player to check
     * @param stargate the stargate to check permissions for
     * @param network the network to check permissions for
     * @param permissionType the type of permission to check
     * @return true if the player has the permission, false otherwise
     */
    public static boolean checkWXPermissions(final Player player, final Stargate stargate, 
                                           final String network, final PermissionType permissionType) {
        if (player == null) {
            return false;
        }

        // Check if Vault permissions are enabled and available
        if (VaultPermissions.isEnabled()) {
            String permissionNode = buildPermissionNode(stargate, network, permissionType);
            if (permissionNode != null) {
                return VaultPermissions.hasPermission(player, permissionNode) || 
                       player.isOp() || 
                       player.hasPermission("wormhole.bypass");
            }
        }
        
        // Fall back to built-in permissions if Vault is not available
        if (WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.BUILTIN_PERMISSIONS_ENABLED)) {
            return PermissionsManager.checkWXPermissions(player, stargate, network, permissionType);
        }
        
        // Default to true if no permission system is enabled
        return true;
    }
    
    /**
     * Build the permission node string based on the stargate, network, and permission type.
     * 
     * @param stargate the stargate (can be null)
     * @param network the network name (can be null)
     * @param permissionType the type of permission
     * @return the permission node string, or null if invalid
     */
    private static String buildPermissionNode(Stargate stargate, String network, PermissionType permissionType) {
        if (permissionType == null) {
            return null;
        }
        
        StringBuilder node = new StringBuilder("wormhole.");
        
        // Add network if available
        if (network != null && !network.isEmpty()) {
            node.append("network.").append(network.toLowerCase()).append(".");
        } 
        // Or add stargate name if available
        else if (stargate != null && stargate.getGateName() != null) {
            node.append("gate.").append(stargate.getGateName().toLowerCase()).append(".");
        }
        
        // Add permission type
        switch (permissionType) {
            case DAMAGE:
                node.append("damage");
                break;
            case SIGN:
                node.append("sign");
                break;
            case DIALER:
                node.append("dialer");
                break;
            case BUILD:
                node.append("build");
                break;
            case REMOVE:
                node.append("remove");
                break;
            case USE:
                node.append("use");
                break;
            case LIST:
                node.append("list");
                break;
            case CONFIG:
                node.append("config");
                break;
            case GO:
                node.append("go");
                break;
            case COMPASS:
                node.append("compass");
                break;
            case USE_COOLDOWN_GROUP_ONE:
                node.append("cooldown.group1");
                break;
            case USE_COOLDOWN_GROUP_TWO:
                node.append("cooldown.group2");
                break;
            case USE_COOLDOWN_GROUP_THREE:
                node.append("cooldown.group3");
                break;
            case BUILD_RESTRICTION_GROUP_ONE:
                node.append("restrict.build.group1");
                break;
            case BUILD_RESTRICTION_GROUP_TWO:
                node.append("restrict.build.group2");
                break;
            case BUILD_RESTRICTION_GROUP_THREE:
                node.append("restrict.build.group3");
                break;
            default:
                return null;
        }
        
        return node.toString();
    }

    /**
     * Check if a player has a specific permission for a network.
     * 
     * @param player the player to check
     * @param network the network to check permissions for
     * @param permissionType the type of permission to check
     * @return true if the player has the permission, false otherwise
     */
    public static boolean checkWXPermissions(final Player player, final String network, final PermissionType permissionType) {
        return checkWXPermissions(player, null, network, permissionType);
    }
    
    /**
     * Gets the WX permission level for a player.
     * 
     * @param player the player to check
     * @return the permission level
     */
    public static PermissionLevel getWXPermissionsLevel(final Player player) {
        if (player == null) {
            return PermissionLevel.WORMHOLE_NO_PERMISSION;
        }

        // Check Vault permissions first if available
        if (VaultPermissions.isEnabled()) {
            if (VaultPermissions.hasPermission(player, "wormhole.admin") || 
                VaultPermissions.hasPermission(player, "wormhole.*")) {
                return PermissionLevel.WORMHOLE_FULL_PERMISSION;
            } else if (VaultPermissions.hasPermission(player, "wormhole.create")) {
                return PermissionLevel.WORMHOLE_CREATE_PERMISSION;
            } else if (VaultPermissions.hasPermission(player, "wormhole.use")) {
                return PermissionLevel.WORMHOLE_USE_PERMISSION;
            } else if (VaultPermissions.hasPermission(player, "wormhole")) {
                return PermissionLevel.WORMHOLE_USE_PERMISSION;
            } else {
                return PermissionLevel.WORMHOLE_NO_PERMISSION;
            }
        }
        
        // Fall back to built-in permissions if Vault is not available
        if (WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.BUILTIN_PERMISSIONS_ENABLED)) {
            return PermissionsManager.getPermissionLevel(player, null);
        }
        
        // Default to full permission if no permission system is enabled
        return PermissionLevel.WORMHOLE_FULL_PERMISSION;
    }
}
