package com.wormhole_xtreme.wormhole.config;

import com.wormhole_xtreme.wormhole.WormholeXTreme;
import com.wormhole_xtreme.wormhole.permissions.PermissionsManager.PermissionLevel;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Utility class for migrating from the old configuration system to the new YAML-based one.
 */
public class ConfigMigrationUtil {
    
    /**
     * Migrates the old configuration to the new YAML format.
     * 
     * @param plugin The plugin instance
     * @return true if migration was successful or not needed, false otherwise
     */
    public static boolean migrateOldConfig(WormholeXTreme plugin) {
        File oldConfigFile = new File(plugin.getDataFolder(), "Settings.txt");
        
        // If the old config file doesn't exist, no migration is needed
        if (!oldConfigFile.exists()) {
            return true;
        }
        
        try {
            // Create the new config if it doesn't exist
            WormholeConfig newConfig = new WormholeConfig(plugin);
            boolean changed = false;
            
            // Migrate each setting from the old config to the new one
            if (migrateSetting("BUILT_IN_PERMISSIONS_ENABLED", "permissions.built_in_enabled", newConfig)) {
                changed = true;
            }
            if (migrateSetting("BUILT_IN_DEFAULT_PERMISSION_LEVEL", "permissions.default_level", newConfig)) {
                changed = true;
            }
            if (migrateSetting("PERMISSIONS_SUPPORT_DISABLE", "permissions.disable_external", newConfig)) {
                changed = true;
            }
            if (migrateSetting("SIMPLE_PERMISSIONS", "permissions.simple_mode", newConfig)) {
                changed = true;
            }
            if (migrateSetting("WORMHOLE_USE_IS_TELEPORT", "permissions.use_is_teleport", newConfig)) {
                changed = true;
            }
            if (migrateSetting("TIMEOUT_ACTIVATE", "timeouts.activate", newConfig)) {
                changed = true;
            }
            if (migrateSetting("TIMEOUT_SHUTDOWN", "timeouts.shutdown", newConfig)) {
                changed = true;
            }
            if (migrateSetting("BUILD_RESTRICTION_ENABLED", "build_restriction.enabled", newConfig)) {
                changed = true;
            }
            if (migrateSetting("BUILD_RESTRICTION_GROUP_ONE", "build_restriction.group_one", newConfig)) {
                changed = true;
            }
            if (migrateSetting("BUILD_RESTRICTION_GROUP_TWO", "build_restriction.group_two", newConfig)) {
                changed = true;
            }
            if (migrateSetting("BUILD_RESTRICTION_GROUP_THREE", "build_restriction.group_three", newConfig)) {
                changed = true;
            }
            if (migrateSetting("USE_COOLDOWN_ENABLED", "cooldown.enabled", newConfig)) {
                changed = true;
            }
            if (migrateSetting("USE_COOLDOWN_GROUP_ONE", "cooldown.group_one", newConfig)) {
                changed = true;
            }
            if (migrateSetting("USE_COOLDOWN_GROUP_TWO", "cooldown.group_two", newConfig)) {
                changed = true;
            }
            if (migrateSetting("USE_COOLDOWN_GROUP_THREE", "cooldown.group_three", newConfig)) {
                changed = true;
            }
            if (migrateSetting("HELP_SUPPORT_DISABLE", "help.disable", newConfig)) {
                changed = true;
            }
            if (migrateSetting("WORLDS_SUPPORT_ENABLED", "worlds.enabled", newConfig)) {
                changed = true;
            }
            if (migrateSetting("LOG_LEVEL", "logging.level", newConfig)) {
                changed = true;
            }
            
            // Save the new config if any changes were made
            if (changed) {
                newConfig.save();
                plugin.prettyLog(Level.INFO, false, "Successfully migrated configuration to new YAML format.");
                
                // Optionally, you can rename the old config file to mark it as migrated
                File backupFile = new File(plugin.getDataFolder(), "Settings.old");
                if (oldConfigFile.renameTo(backupFile)) {
                    plugin.prettyLog(Level.INFO, false, "Old configuration file has been renamed to 'Settings.old'.");
                }
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.prettyLog(Level.SEVERE, false, "Failed to migrate configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Migrates a single setting from the old config to the new one.
     * 
     * @param oldKey The old configuration key
     * @param newKey The new configuration key path
     * @param config The new config instance
     * @return true if the setting was migrated, false otherwise
     */
    private static boolean migrateSetting(String oldKey, String newKey, WormholeConfig config) {
        try {
            // Get the old value from ConfigManager
            Object oldValue = getOldConfigValue(oldKey);
            if (oldValue == null) {
                return false;
            }
            
            // Update the new config with the old value
            switch (newKey) {
                case "permissions.built_in_enabled":
                case "permissions.disable_external":
                case "permissions.simple_mode":
                case "permissions.use_is_teleport":
                case "build_restriction.enabled":
                case "cooldown.enabled":
                case "help.disable":
                case "worlds.enabled":
                    config.set(WormholeConfig.BUILT_IN_PERMISSIONS_ENABLED, (Boolean) oldValue);
                    break;
                    
                case "permissions.default_level":
                    if (oldValue instanceof PermissionLevel) {
                        config.set(WormholeConfig.BUILT_IN_DEFAULT_PERMISSION_LEVEL, ((PermissionLevel) oldValue).name());
                    } else if (oldValue instanceof String) {
                        try {
                            PermissionLevel level = PermissionLevel.valueOf((String) oldValue);
                            config.set(WormholeConfig.BUILT_IN_DEFAULT_PERMISSION_LEVEL, level.name());
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                    break;
                    
                case "timeouts.activate":
                case "timeouts.shutdown":
                case "build_restriction.group_one":
                case "build_restriction.group_two":
                case "build_restriction.group_three":
                case "cooldown.group_one":
                case "cooldown.group_two":
                case "cooldown.group_three":
                    if (oldValue instanceof Number) {
                        config.set(WormholeConfig.TIMEOUT_ACTIVATE, ((Number) oldValue).intValue());
                    } else if (oldValue instanceof String) {
                        try {
                            int value = Integer.parseInt((String) oldValue);
                            config.set(WormholeConfig.TIMEOUT_ACTIVATE, value);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                    break;
                    
                case "logging.level":
                    config.set(WormholeConfig.LOG_LEVEL, oldValue.toString());
                    break;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets a value from the old configuration system.
     * 
     * @param key The configuration key
     * @return The configuration value, or null if not found
     */
    private static Object getOldConfigValue(String key) {
        // This is a simplified example - you'll need to adapt this to your old config system
        try {
            // Use reflection to access the old ConfigManager if it's still in the classpath
            Class<?> configManager = Class.forName("com.wormhole_xtreme.wormhole.config.ConfigManager");
            java.lang.reflect.Method getSetting = configManager.getMethod("getSetting", String.class);
            Object setting = getSetting.invoke(null, key);
            
            if (setting != null) {
                java.lang.reflect.Method getValue = setting.getClass().getMethod("getValue");
                return getValue.invoke(setting);
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
}
