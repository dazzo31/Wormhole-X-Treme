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
import java.util.logging.Logger;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import com.wormhole_xtreme.worlds.handler.WorldHandler;
import com.wormhole_xtreme.wormhole.command.Build;
import com.wormhole_xtreme.wormhole.command.Compass;
import com.wormhole_xtreme.wormhole.command.Complete;
import com.wormhole_xtreme.wormhole.command.Dial;
import com.wormhole_xtreme.wormhole.command.Force;
import com.wormhole_xtreme.wormhole.command.Go;
import com.wormhole_xtreme.wormhole.command.WXIDC;
import com.wormhole_xtreme.wormhole.command.WXList;
import com.wormhole_xtreme.wormhole.command.WXRemove;
import com.wormhole_xtreme.wormhole.command.Wormhole;
import com.wormhole_xtreme.wormhole.config.WormholeConfig;
import com.wormhole_xtreme.wormhole.logic.StargateHelper;
import com.wormhole_xtreme.wormhole.model.Stargate;
import com.wormhole_xtreme.wormhole.database.DatabaseInitializer;
import com.wormhole_xtreme.wormhole.database.DatabaseManager;
import com.wormhole_xtreme.wormhole.database.StargateRepository;
import com.wormhole_xtreme.wormhole.model.StargateManager;
import com.wormhole_xtreme.wormhole.permissions.PermissionsManager;
import com.wormhole_xtreme.wormhole.plugin.HelpSupport;
import com.wormhole_xtreme.wormhole.plugin.PermissionsSupport;
import com.wormhole_xtreme.wormhole.plugin.WormholeWorldsSupport;
import com.wormhole_xtreme.wormhole.utils.DBUpdateUtil;

/**
 * WormholeXtreme for Bukkit.
 * 
 * @author Ben Echols (Lologarithm)
 * @author Dean Bailey (alron)
 */
public class WormholeXTreme extends JavaPlugin {
    // Vault Services
    private static Economy econ = null;
    private static Permission perms = null;
    private static Chat chat = null;
    private static boolean vaultEnabled = false;

    // Configuration
    private WormholeConfig wormholeConfig;
    
    // Database
    private DatabaseManager databaseManager;
    private DatabaseInitializer databaseInitializer;
    private StargateRepository stargateRepository;

    // Plugin instances
    private static WormholeXTreme instance;
    private static Logger log;
    private static BukkitScheduler scheduler;
    private static WorldHandler worldHandler;

    // Listeners
    private final WormholeXTremePlayerListener playerListener = new WormholeXTremePlayerListener(this);
    private final WormholeXTremeBlockListener blockListener = new WormholeXTremeBlockListener(this);
    private final WormholeXTremeVehicleListener vehicleListener = new WormholeXTremeVehicleListener(this);
    private final WormholeXTremeEntityListener entityListener = new WormholeXTremeEntityListener(this);
    private final WormholeXTremeServerListener serverListener = new WormholeXTremeServerListener(this);
    private final WormholeXTremeRedstoneListener redstoneListener = new WormholeXTremeRedstoneListener(this);

    /**
     * Gets the plugin instance.
     *
     * @return the plugin instance
     */
    public static WormholeXTreme getInstance() {
        return instance;
    }

    /**
     * Gets the plugin configuration.
     *
     * @return the plugin configuration
     */
    public WormholeConfig getWormholeConfig() {
        return wormholeConfig;
    }

    /**
     * Gets the database manager instance.
     *
     * @return the database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Gets the stargate repository.
     *
     * @return the stargate repository
     */
    public StargateRepository getStargateRepository() {
        return stargateRepository;
    }

    /**
     * Initializes the database connection and schema.
     *
     * @return true if initialization was successful, false otherwise
     */
    private boolean initializeDatabase() {
        try {
            // Initialize database manager
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                getLogger().severe("Failed to initialize database manager");
                return false;
            }

            // Initialize database schema
            databaseInitializer = new DatabaseInitializer(this, databaseManager);
            if (!databaseInitializer.initialize()) {
                getLogger().severe("Failed to initialize database schema");
                return false;
            }

            // Initialize repositories
            stargateRepository = new StargateRepository(this, databaseManager);

            getLogger().info("Database initialized successfully");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing database", e);
            return false;
        }
    }

    /**
     * Gets the logger.
     *
     * @return the logger instance
     */
    public static Logger getLog() {
        return log;
    }

    /**
     * Gets the Vault Economy instance.
     *
     * @return the economy instance
     */
    public static Economy getEconomy() {
        return econ;
    }

    /**
     * Gets the Vault Permissions instance.
     *
     * @return the permissions instance
     */
    public static Permission getPermissions() {
        return perms;
    }

    /**
     * Gets the Vault Chat instance.
     *
     * @return the chat instance
     */
    public static Chat getChat() {
        return chat;
    }

    /**
     * Gets the Bukkit scheduler.
     *
     * @return the scheduler instance
     */
    public static BukkitScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Gets the WorldHandler instance.
     *
     * @return the world handler instance
     */
    public static WorldHandler getWorldHandler() {
        return worldHandler;
    }

    /**
     * Register commands.
     */
    private void registerCommands() {
        Objects.requireNonNull(getCommand("wxforce")).setExecutor(new Force(this));
        Objects.requireNonNull(getCommand("wxidc")).setExecutor(new WXIDC(this));
        Objects.requireNonNull(getCommand("wxcompass")).setExecutor(new Compass(this));
        Objects.requireNonNull(getCommand("wxcomplete")).setExecutor(new Complete(this));
        Objects.requireNonNull(getCommand("wxremove")).setExecutor(new WXRemove(this));
        Objects.requireNonNull(getCommand("wxlist")).setExecutor(new WXList(this));
        Objects.requireNonNull(getCommand("wxgo")).setExecutor(new Go(this));
        Objects.requireNonNull(getCommand("dial")).setExecutor(new Dial(this));
        Objects.requireNonNull(getCommand("wxbuild")).setExecutor(new Build(this));
        Objects.requireNonNull(getCommand("wormhole")).setExecutor(new Wormhole(this));
    }

    /**
     * Register event listeners.
     */
    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        // Register event listeners
        pm.registerEvents(playerListener, this);
        pm.registerEvents(blockListener, this);
        pm.registerEvents(vehicleListener, this);
        pm.registerEvents(entityListener, this);
        pm.registerEvents(serverListener, this);
        pm.registerEvents(redstoneListener, this);
    }

    /**
     * Set up Vault integration.
     *
     * @return true if Vault was set up successfully
     */
    private boolean setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLog().warning("Vault not found. Some features may not work correctly.");
            return false;
        }

        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp != null) {
            perms = rsp.getProvider();
        }

        RegisteredServiceProvider<Economy> rspEcon = getServer().getServicesManager().getRegistration(Economy.class);
        if (rspEcon != null) {
            econ = rspEcon.getProvider();
        }

        RegisteredServiceProvider<Chat> rspChat = getServer().getServicesManager().getRegistration(Chat.class);
        if (rspChat != null) {
            chat = rspChat.getProvider();
        }

        return true;
    } catch (Exception e) {
        getLogger().log(Level.SEVERE, "Error initializing database", e);
        return false;
    }
}

/**
 * Gets the logger.
 *
 * @return the logger instance
 */
public static Logger getLog() {
    return log;
}

/**
 * Gets the Vault Economy instance.
 *
 * @return the economy instance
 */
public static Economy getEconomy() {
    return econ;
}

/**
 * Gets the Vault Permissions instance.
 *
 * @return the permissions instance
 */
public static Permission getPermissions() {
    return perms;
}

/**
 * Gets the Vault Chat instance.
 *
 * @return the chat instance
 */
public static Chat getChat() {
    return chat;
}

/**
 * Gets the Bukkit scheduler.
 *
 * @return the scheduler instance
 */
public static BukkitScheduler getScheduler() {
    return scheduler;
}

/**
 * Gets the WorldHandler instance.
 *
 * @return the world handler instance
 */
public static WorldHandler getWorldHandler() {
    return worldHandler;
}

/**
 * Register commands.
 */
private void registerCommands() {
    Objects.requireNonNull(getCommand("wxforce")).setExecutor(new Force(this));
    Objects.requireNonNull(getCommand("wxidc")).setExecutor(new WXIDC(this));
    Objects.requireNonNull(getCommand("wxcompass")).setExecutor(new Compass(this));
    Objects.requireNonNull(getCommand("wxcomplete")).setExecutor(new Complete(this));
    Objects.requireNonNull(getCommand("wxremove")).setExecutor(new WXRemove(this));
    Objects.requireNonNull(getCommand("wxlist")).setExecutor(new WXList(this));
    Objects.requireNonNull(getCommand("wxgo")).setExecutor(new Go(this));
    Objects.requireNonNull(getCommand("dial")).setExecutor(new Dial(this));
    Objects.requireNonNull(getCommand("wxbuild")).setExecutor(new Build(this));
    Objects.requireNonNull(getCommand("wormhole")).setExecutor(new Wormhole(this));
}

/**
 * Register event listeners.
 */
private void registerListeners() {
    PluginManager pm = getServer().getPluginManager();

    // Register event listeners
    pm.registerEvents(playerListener, this);
    pm.registerEvents(blockListener, this);
    pm.registerEvents(vehicleListener, this);
    pm.registerEvents(entityListener, this);
    pm.registerEvents(serverListener, this);
    pm.registerEvents(redstoneListener, this);
}

/**
 * Set up Vault integration.
 *
 * @return true if Vault was set up successfully
 */
private boolean setupVault() {
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
        getLog().warning("Vault not found. Some features may not work correctly.");
        return false;
    }

    RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
    if (rsp != null) {
        perms = rsp.getProvider();
    }

    RegisteredServiceProvider<Economy> rspEcon = getServer().getServicesManager().getRegistration(Economy.class);
    if (rspEcon != null) {
        econ = rspEcon.getProvider();
    }

    RegisteredServiceProvider<Chat> rspChat = getServer().getServicesManager().getRegistration(Chat.class);
    if (rspChat != null) {
        chat = rspChat.getProvider();
    }

    return true;
}

@Override
public void onEnable() {
    // Set up plugin instance and logger
    instance = this;
    log = getLogger();
    scheduler = getServer().getScheduler();
    
    // Set up configuration
    try {
        // Initialize new config
        wormholeConfig = new WormholeConfig(this);
        
        // Migrate from old config if needed
        ConfigMigrationUtil.migrateOldConfig(this);
        
    } catch (Exception e) {
        getLogger().log(Level.SEVERE, "Failed to load configuration", e);
        getServer().getPluginManager().disablePlugin(this);
        return;
    }
    
    // Set up Vault
    vaultEnabled = setupVault();
    
    // Set up database
    if (!initializeDatabase()) {
        getLogger().severe("Failed to initialize database! Disabling plugin...");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }
    
    // Register commands and event listeners
    registerCommands();
    registerListeners();
    
    // Set up permissions
    setupPermissions();
    
    // Set up world handler if enabled
    if (wormholeConfig.get(WormholeConfig.WORLDS_SUPPORT_ENABLED)) {
        try {
            worldHandler = new WorldHandler(this);
            prettyLog(Level.INFO, false, "Wormhole Worlds support enabled.");
        } catch (Exception e) {
            prettyLog(Level.WARNING, false, "Failed to initialize Wormhole Worlds support: " + e.getMessage());
        }
    }
    
    // Load stargates from database
    StargateManager.loadAllStargates();
    
    prettyLog(Level.INFO, true, "has been enabled!");
}

@Override
public void onDisable() {
    // Plugin shutdown logic
    if (databaseManager != null) {
        databaseManager.shutdown();
    }

    // Save any pending data
    StargateManager.saveAllGates();

    // Cancel all tasks
    scheduler.cancelTasks(this);

    // Close database connection
    StargateDBManager.closeConnection();

    getLog().info("WormholeXTreme has been disabled!");
}

/**
 * Reloads the plugin configuration and stargates.
 */
public void reload() {
    try {
        // Reload configuration

        // Save any pending data
        StargateManager.saveAllGates();

        // Cancel all tasks
        scheduler.cancelTasks(this);

        // Close database connection
        StargateDBManager.closeConnection();

        getLog().info("WormholeXTreme has been disabled!");
    }

    /**
     * Reloads the plugin configuration and stargates.
     */
    public void reload() {
        wormholeConfig.reload();
        prettyLog(Level.INFO, false, "Configuration reloaded.");
            }
            
            HelpSupport.enableHelp();
            if (wormholeConfig.get(WormholeConfig.WORLD_SUPPORT_ENABLED))
            {
                WormholeWorldsSupport.enableWormholeWorlds();
            }
        }
        catch (final Exception e)
        {
            prettyLog(Level.WARNING, false, "Caught Exception while trying to load support plugins: " + e.getMessage());
            e.printStackTrace();
        }
        
        registerEvents(true);
        HelpSupport.registerHelpCommands();
        if (!wormholeConfig.get(WormholeConfig.WORLD_SUPPORT_ENABLED))
        {
            registerEvents(false);
            registerCommands();
            prettyLog(Level.INFO, true, "Enable Completed.");
        }
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#onLoad()
     */
    @Override
    public void onLoad()
    {
        setThisPlugin(this);
        setLog(getThisPlugin().getServer().getLogger());
        setScheduler(getThisPlugin().getServer().getScheduler());

        prettyLog(Level.INFO, true, "Load Beginning.");
        // Load our config files and set logging level right away.
        wormholeConfig = new WormholeConfig(this);
        wormholeConfig.load();
        WormholeXTreme.setPrettyLogLevel(wormholeConfig.get(WormholeConfig.LOG_LEVEL));
        // Make sure DB is up to date with latest SCHEMA
        DBUpdateUtil.updateDB();
        // Load our shapes, stargates, and internal permissions.
        StargateHelper.loadShapes();
        if (!wormholeConfig.get(WormholeConfig.WORLD_SUPPORT_ENABLED))
        {
            prettyLog(Level.INFO, true, "Wormhole Worlds support disabled in settings.txt, loading stargates and worlds ourself.");
            StargateDBManager.loadStargates(getThisPlugin().getServer());
        }
        PermissionsManager.loadPermissions();
        prettyLog(Level.INFO, true, "Load Completed.");
    }

    /**
     * 
     * prettyLog: A quick and dirty way to make log output clean, unified, and with versioning as needed.
     * 
     * @param severity
     *            Level of severity in the form of INFO, WARNING, SEVERE, etc.
     * @param version
     *            true causes version display in log entries.
     * @param message
     *            to prettyLog.
     * 
     */
    public void prettyLog(final Level severity, final boolean version, final String message)
    {
        final String prettyName = ("[" + getThisPlugin().getDescription().getName() + "]");
        final String prettyVersion = ("[v" + getThisPlugin().getDescription().getVersion() + "]");
        String prettyLogLine = prettyName;
        if (version)
        {
            prettyLogLine += prettyVersion;
            getLog().log(severity, prettyLogLine + " " + message);
        }
        else
        {
            getLog().log(severity, prettyLogLine + " " + message);
        }
    }

}
