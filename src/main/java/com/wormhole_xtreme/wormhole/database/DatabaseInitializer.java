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

import com.wormhole_xtreme.wormhole.WormholeXTreme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Handles database schema initialization and updates.
 */
public class DatabaseInitializer {
    private static final int CURRENT_DB_VERSION = 6;
    private static final String DB_UPDATE_PATH = "/sql_commands/db_create_";
    
    private final WormholeXTreme plugin;
    private final DatabaseManager databaseManager;
    
    /**
     * Creates a new DatabaseInitializer instance.
     *
     * @param plugin The plugin instance
     * @param databaseManager The database manager
     */
    public DatabaseInitializer(WormholeXTreme plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    /**
     * Initializes the database schema and applies any necessary updates.
     *
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        try (Connection conn = databaseManager.getConnection()) {
            // Create VersionInfo table if it doesn't exist
            if (!tableExists(conn, "VersionInfo")) {
                executeSql(conn, "CREATE TABLE VersionInfo (Version INTEGER);");
                executeSql(conn, "INSERT INTO VersionInfo VALUES (0);");
            }
            
            // Get current version
            int currentVersion = getCurrentVersion(conn);
            
            // Apply updates if needed
            if (currentVersion < CURRENT_DB_VERSION) {
                plugin.getLogger().info("Updating database from version " + currentVersion + " to " + CURRENT_DB_VERSION + "...");
                
                for (int version = currentVersion + 1; version <= CURRENT_DB_VERSION; version++) {
                    String resourcePath = DB_UPDATE_PATH + version;
                    List<String> updateStatements = loadSqlStatements(resourcePath);
                    
                    try {
                        executeSqlBatch(conn, updateStatements);
                        plugin.getLogger().info("Applied database update to version " + version);
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to apply database update to version " + version, e);
                        return false;
                    }
                }
            }
            
            // Verify all required tables exist
            if (!verifySchema(conn)) {
                plugin.getLogger().severe("Database schema verification failed");
                return false;
            }
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    /**
     * Verifies that all required tables exist in the database.
     *
     * @param conn The database connection
     * @return true if all required tables exist, false otherwise
     * @throws SQLException if a database error occurs
     */
    private boolean verifySchema(Connection conn) throws SQLException {
        String[] requiredTables = {"VersionInfo", "Stargates", "StargateIndividualPermissions", "StargateGroupPermissions"};
        
        for (String table : requiredTables) {
            if (!tableExists(conn, table)) {
                plugin.getLogger().severe("Required table does not exist: " + table);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a table exists in the database.
     *
     * @param conn The database connection
     * @param tableName The name of the table to check
     * @return true if the table exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName.toUpperCase(), null)) {
            return rs.next();
        }
    }
    
    /**
     * Gets the current database version.
     *
     * @param conn The database connection
     * @return The current database version, or 0 if not found
     * @throws SQLException if a database error occurs
     */
    private int getCurrentVersion(Connection conn) throws SQLException {
        if (!tableExists(conn, "VersionInfo")) {
            return 0;
        }
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(Version) as version FROM VersionInfo")) {
            
            if (rs.next()) {
                return rs.getInt("version");
            }
            return 0;
        }
    }
    
    /**
     * Loads SQL statements from a resource file.
     *
     * @param resourcePath The path to the resource file
     * @return A list of SQL statements
     */
    private List<String> loadSqlStatements(String resourcePath) {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                
                // Add line to current statement
                currentStatement.append(line);
                
                // If line ends with semicolon, it's the end of a statement
                if (line.endsWith(";")) {
                    statements.add(currentStatement.toString());
                    currentStatement.setLength(0);
                } else {
                    // Add space between lines of the same statement
                    currentStatement.append(" ");
                }
            }
            
            // Add any remaining statement that doesn't end with a semicolon
            if (currentStatement.length() > 0) {
                statements.add(currentStatement.toString());
            }
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load SQL statements from resource: " + resourcePath, e);
        }
        
        return statements;
    }
    
    /**
     * Executes a single SQL statement.
     *
     * @param conn The database connection
     * @param sql The SQL statement to execute
     * @throws SQLException if a database error occurs
     */
    private void executeSql(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    /**
     * Executes a batch of SQL statements in a transaction.
     *
     * @param conn The database connection
     * @param statements The SQL statements to execute
     * @throws SQLException if a database error occurs
     */
    private void executeSqlBatch(Connection conn, List<String> statements) throws SQLException {
        boolean autoCommit = conn.getAutoCommit();
        try {
            if (autoCommit) {
                conn.setAutoCommit(false);
            }
            
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    stmt.addBatch(sql);
                }
                stmt.executeBatch();
            }
            
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            if (autoCommit) {
                conn.setAutoCommit(true);
            }
        }
    }
}
