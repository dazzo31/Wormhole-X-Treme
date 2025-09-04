package com.wormhole_xtreme.wormhole.permissions;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.permission.Permission;

import com.wormhole_xtreme.wormhole.WormholeXTreme;

/**
 * Handles Vault permissions integration for WormholeXTreme.
 */
public class VaultPermissions {
    
    private static Permission permission = null;
    private static boolean enabled = false;
    
    /**
     * Initialize Vault permissions.
     *
     * @param plugin the plugin instance
     * @return true if Vault permissions were successfully initialized
     */
    public static boolean initialize(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found. Using built-in permissions system.");
            return false;
        }
        
        RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            plugin.getLogger().warning("No permission service found. Using built-in permissions system.");
            return false;
        }
        
        permission = rsp.getProvider();
        enabled = true;
        plugin.getLogger().info("Hooked into Vault permissions: " + permission.getName());
        return true;
    }
    
    /**
     * Check if Vault permissions are enabled.
     *
     * @return true if Vault permissions are enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if a player has a permission node.
     *
     * @param player the player to check
     * @param permissionNode the permission node to check
     * @return true if the player has the permission
     */
    public static boolean hasPermission(Player player, String permissionNode) {
        if (!enabled || player == null || permissionNode == null) {
            return false;
        }
        
        try {
            return permission.has(player, permissionNode);
        } catch (Exception e) {
            WormholeXTreme.getThisPlugin().getLogger().warning("Error checking Vault permission: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the Vault permission provider instance.
     *
     * @return the Vault permission provider, or null if not available
     */
    public static Permission getPermission() {
        return permission;
    }
}
