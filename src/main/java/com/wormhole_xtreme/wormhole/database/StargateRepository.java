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
import com.wormhole_xtreme.wormhole.model.Stargate;
import org.bukkit.World;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Handles database operations for Stargate entities.
 */
public class StargateRepository {
    private final DatabaseManager databaseManager;
    private final WormholeXTreme plugin;
    
    /**
     * Creates a new StargateRepository instance.
     *
     * @param plugin The plugin instance
     * @param databaseManager The database manager
     */
    public StargateRepository(WormholeXTreme plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    /**
     * Saves a stargate to the database.
     *
     * @param stargate The stargate to save
     * @return true if the operation was successful, false otherwise
     */
    public boolean saveStargate(Stargate stargate) {
        String sql = """
            MERGE INTO Stargates (
                Id, Name, GateData, Network, World, WorldName, WorldEnvironment, Owner, GateShape
            ) KEY (Id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""";
            
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setObject(1, stargate.getGateId() > 0 ? stargate.getGateId() : null);
            stmt.setString(2, stargate.getGateName());
            stmt.setBytes(3, stargate.toByteArray());
            stmt.setString(4, stargate.getGateNetwork() != null ? stargate.getGateNetwork().getNetworkName() : null);
            stmt.setLong(5, stargate.getGateWorld().getUID().getMostSignificantBits());
            stmt.setString(6, stargate.getGateWorld().getName());
            stmt.setString(7, stargate.getGateWorld().getEnvironment().name());
            stmt.setString(8, stargate.getGateOwner());
            stmt.setString(9, stargate.getGateShape() != null ? stargate.getGateShape().getShapeName() : "Standard");
            
            int affectedRows = stmt.executeUpdate();
            
            // If this is a new stargate, get the generated ID
            if (stargate.getGateId() <= 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        stargate.setGateId(generatedKeys.getInt(1));
                    }
                }
            }
            
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save stargate: " + stargate.getGateName(), e);
            return false;
        }
    }
    
    /**
     * Deletes a stargate from the database.
     *
     * @param stargate The stargate to delete
     * @return true if the operation was successful, false otherwise
     */
    public boolean deleteStargate(Stargate stargate) {
        String sql = "DELETE FROM Stargates WHERE Id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, stargate.getGateId());
            int affectedRows = stmt.executeUpdate();
            
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete stargate: " + stargate.getGateName(), e);
            return false;
        }
    }
    
    /**
     * Finds a stargate by its ID.
     *
     * @param id The ID of the stargate to find
     * @return An Optional containing the stargate if found, or empty if not found
     */
    public Optional<Stargate> findById(int id) {
        String sql = "SELECT * FROM Stargates WHERE Id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStargate(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to find stargate by ID: " + id, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Finds a stargate by its name.
     *
     * @param name The name of the stargate to find
     * @return An Optional containing the stargate if found, or empty if not found
     */
    public Optional<Stargate> findByName(String name) {
        String sql = "SELECT * FROM Stargates WHERE Name = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStargate(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to find stargate by name: " + name, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Finds all stargates in the database.
     *
     * @return A list of all stargates
     */
    public List<Stargate> findAll() {
        List<Stargate> stargates = new ArrayList<>();
        String sql = "SELECT * FROM Stargates";
        
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                try {
                    stargates.add(mapResultSetToStargate(rs));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load stargate with ID: " + rs.getInt("Id"), e);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load stargates", e);
        }
        
        return stargates;
    }
    
    /**
     * Finds all stargates in a specific world.
     *
     * @param world The world to search in
     * @return A list of stargates in the specified world
     */
    public List<Stargate> findByWorld(World world) {
        List<Stargate> stargates = new ArrayList<>();
        String sql = "SELECT * FROM Stargates WHERE WorldName = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, world.getName());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        stargates.add(mapResultSetToStargate(rs));
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to load stargate with ID: " + rs.getInt("Id"), e);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load stargates for world: " + world.getName(), e);
        }
        
        return stargates;
    }
    
    /**
     * Maps a ResultSet row to a Stargate object.
     *
     * @param rs The ResultSet containing the stargate data
     * @return A Stargate object
     * @throws SQLException if a database error occurs
     */
    private Stargate mapResultSetToStargate(ResultSet rs) throws SQLException {
        // TODO: Implement proper mapping from ResultSet to Stargate
        // This is a placeholder - the actual implementation will depend on your Stargate class
        // and how it's structured.
        Stargate stargate = new Stargate();
        stargate.setGateId(rs.getInt("Id"));
        stargate.setGateName(rs.getString("Name"));
        // Set other properties as needed
        
        return stargate;
    }
}
