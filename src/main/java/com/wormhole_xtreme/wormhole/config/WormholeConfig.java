package com.wormhole_xtreme.wormhole.config;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.migration.PlainMigrationService;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.PropertyInitializer;
import ch.jalu.configme.resource.YamlFileResource;
import com.wormhole_xtreme.wormhole.WormholeXTreme;
import com.wormhole_xtreme.wormhole.permissions.PermissionsManager.PermissionLevel;

import java.io.File;

/**
 * Configuration class for Wormhole X-Treme plugin using ConfigMe.
 */
public class WormholeConfig {
    
    // Property definitions with default values and comments
    public static final Property<Integer> TIMEOUT_ACTIVATE = PropertyInitializer.newProperty("timeouts.activate", 30,
            "Number of seconds after a gate is activated, but before dialing before timing out.");
    
    public static final Property<Integer> TIMEOUT_SHUTDOWN = PropertyInitializer.newProperty("timeouts.shutdown", 38,
            "Number of seconds after a gate is dialed before automatically shutdown. With 0 timeout a gate won't shutdown until something goes through the gate.");
    
    public static final Property<Boolean> BUILD_RESTRICTION_ENABLED = PropertyInitializer.newProperty("build_restriction.enabled", false,
            "Enable build count restrictions. Requires complex permissions.");
    
    public static final Property<Integer> BUILD_RESTRICTION_GROUP_ONE = PropertyInitializer.newProperty("build_restriction.group_one", 1,
            "Total number of stargates a member of build restriction group one can build.");
    
    public static final Property<Integer> BUILD_RESTRICTION_GROUP_TWO = PropertyInitializer.newProperty("build_restriction.group_two", 2,
            "Total number of stargates a member of build restriction group two can build.");
    
    public static final Property<Integer> BUILD_RESTRICTION_GROUP_THREE = PropertyInitializer.newProperty("build_restriction.group_three", 3,
            "Total number of stargates a member of build restriction group three can build.");
    
    public static final Property<Boolean> USE_COOLDOWN_ENABLED = PropertyInitializer.newProperty("cooldown.enabled", false,
            "Enable Cooldown timers on stargate usage. Timer only activates on passage through wormholes.");
    
    public static final Property<Integer> USE_COOLDOWN_GROUP_ONE = PropertyInitializer.newProperty("cooldown.group_one", 120,
            "Cooldown time in seconds between stargate use for members of use cooldown group one.");
    
    public static final Property<Integer> USE_COOLDOWN_GROUP_TWO = PropertyInitializer.newProperty("cooldown.group_two", 60,
            "Cooldown time in seconds between stargate use for members of use cooldown group two.");
    
    public static final Property<Integer> USE_COOLDOWN_GROUP_THREE = PropertyInitializer.newProperty("cooldown.group_three", 30,
            "Cooldown time in seconds between stargate use for members of use cooldown group three.");
    
    public static final Property<Boolean> BUILT_IN_PERMISSIONS_ENABLED = PropertyInitializer.newProperty("permissions.built_in_enabled", false,
            "Enable built-in permissions. Only used if no permissions plugin is detected.");
    
    public static final Property<String> BUILT_IN_DEFAULT_PERMISSION_LEVEL = PropertyInitializer.newProperty("permissions.default_level", "WORMHOLE_USE_PERMISSION",
            "Default permission level for players if built-in permissions are used. Values: WORMHOLE_USE_PERMISSION, WORMHOLE_BUILD_PERMISSION, WORMHOLE_CONFIG_PERMISSION, WORMHOLE_ADMIN_PERMISSION");
    
    public static final Property<Boolean> PERMISSIONS_SUPPORT_DISABLE = PropertyInitializer.newProperty("permissions.disable_external", false,
            "If true, external permissions plugins will be ignored even if available.");
    
    public static final Property<Boolean> SIMPLE_PERMISSIONS = PropertyInitializer.newProperty("permissions.simple_mode", false,
            "Use simplified permission nodes (wormhole.simple.*) instead of the full permission set.");
    
    public static final Property<Boolean> WORMHOLE_USE_IS_TELEPORT = PropertyInitializer.newProperty("permissions.use_is_teleport", false,
            "If true, wormhole.use permission is required to teleport. If false, only needed to activate gates.");
    
    public static final Property<Boolean> HELP_SUPPORT_DISABLE = PropertyInitializer.newProperty("help.disable", false,
            "If true, Help plugin integration will be disabled.");
    
    public static final Property<Boolean> WORLDS_SUPPORT_ENABLED = PropertyInitializer.newProperty("worlds.enabled", false,
            "If true, Wormhole X-Treme Worlds will be used for world management.");
    
    public static final Property<String> LOG_LEVEL = PropertyInitializer.newProperty("logging.level", "INFO",
            "Logging level. Values: SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST");
    
    private final SettingsManager settingsManager;
    
    /**
     * Creates a new WormholeConfig instance.
     *
     * @param plugin The plugin instance
     */
    public WormholeConfig(WormholeXTreme plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        // Create parent directories if they don't exist
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }
        
        this.settingsManager = SettingsManagerBuilder
                .withYamlFile(configFile)
                .configurationData(ConfigurationDataBuilder.collectAnnotations(WormholeConfig.class))
                .migrationService(new PlainMigrationService())
                .create();
        
        // Save the config if it was just created or updated
        settingsManager.save();
    }
    
    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
        settingsManager.reload();
    }
    
    /**
     * Saves the current configuration to disk.
     */
    public void save() {
        settingsManager.save();
    }
    
    /**
     * Gets a property value.
     *
     * @param <T> The property type
     * @param property The property
     * @return The property value
     */
    public <T> T get(Property<T> property) {
        return settingsManager.getProperty(property);
    }
    
    /**
     * Sets a property value.
     *
     * @param <T> The property type
     * @param property The property
     * @param value The new value
     */
    public <T> void set(Property<T> property, T value) {
        settingsManager.setProperty(property, value);
    }
    
    /**
     * Gets the PermissionLevel from the configuration.
     *
     * @return The PermissionLevel
     */
    public PermissionLevel getDefaultPermissionLevel() {
        try {
            return PermissionLevel.valueOf(get(BUILT_IN_DEFAULT_PERMISSION_LEVEL));
        } catch (IllegalArgumentException e) {
            return PermissionLevel.WORMHOLE_USE_PERMISSION; // Default fallback
        }
    }
}
