/*
 *   Wormhole X-Treme Plugin for Bukkit
 *   Copyright (C) 2024 Wormhole X-Treme Team
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
package com.wormhole_xtreme.wormhole.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connections using HikariCP connection pooling.
 */
public class DatabaseManager {
    private static final String DB_PATH = "./plugins/WormholeXTreme/WormholeXTremeDB";
    private static final String DB_NAME = "WormholeXTremeDB";
    private static final String DB_URL = "jdbc:hsqldb:file:" + DB_PATH + "/" + DB_NAME + ";hsqldb.log_data=false;hsqldb.tx=mvcc";
    
    private final JavaPlugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;
    
    /**
     * Creates a new DatabaseManager instance.
     *
     * @param plugin The plugin instance
     */
    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Initializes the database connection pool.
     *
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        try {
            // Create database directory if it doesn't exist
            File dbDir = new File(DB_PATH);
            if (!dbDir.exists() && !dbDir.mkdirs()) {
                logger.severe("Failed to create database directory: " + DB_PATH);
                return false;
            }
            
            // Configure HikariCP
            Properties props = new Properties();
            props.setProperty("dataSourceClassName", "org.hsqldb.jdbc.JDBCDataSource");
            props.setProperty("dataSource.url", DB_URL);
            props.setProperty("dataSource.user", "SA");
            props.setProperty("dataSource.password", "");
            
            // Connection pool settings
            props.setProperty("connectionTimeout", "30000");
            props.setProperty("idleTimeout", "600000");
            props.setProperty("maxLifetime", "1800000");
            props.setProperty("maximumPoolSize", "10");
            props.setProperty("minimumIdle", "1");
            props.setProperty("leakDetectionThreshold", "60000");
            
            HikariConfig config = new HikariConfig(props);
            config.setPoolName("WormholeXTremePool");
            config.setInitializationFailTimeout(-1); // Don't fail fast on startup
            
            dataSource = new HikariDataSource(config);
            
            // Test the connection
            try (Connection conn = getConnection()) {
                logger.info("Successfully connected to the database");
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database connection pool", e);
            return false;
        }
    }
    
    /**
     * Gets a connection from the connection pool.
     *
     * @return A database connection
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection pool is not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Closes the connection pool.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool has been shutdown");
        }
    }
    
    /**
     * Executes a database operation within a transaction.
     *
     * @param operation The database operation to execute
     * @param <T> The return type of the operation
     * @return The result of the operation
     * @throws SQLException if a database error occurs
     */
    public <T> T executeTransaction(DatabaseOperation<T> operation) throws SQLException {
        try (Connection conn = getConnection()) {
            try {
                conn.setAutoCommit(false);
                T result = operation.execute(conn);
                conn.commit();
                return result;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    /**
     * Functional interface for database operations.
     *
     * @param <T> The return type of the operation
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(Connection connection) throws SQLException;
    }
}
