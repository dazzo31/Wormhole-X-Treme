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
package com.wormhole_xtreme.wormhole.model;

import com.wormhole_xtreme.wormhole.WormholeXTreme;
import com.wormhole_xtreme.wormhole.database.StargateRepository;
import com.wormhole_xtreme.wormhole.logic.StargateUpdateRunnable;
import com.wormhole_xtreme.wormhole.logic.StargateUpdateRunnable.ActionToTake;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

// TODO: Auto-generated Javadoc
/**
 * WormholeXtreme Stargate Manager.
 * 
 * @author Ben Echols (Lologarithm)
 */
public class StargateManager
{
    // A list of all blocks contained by all stargates. Makes for easy indexing when a player is trying
    // to enter a gate or if water is trying to flow out, also will contain the stone buttons used to activate.
    private static final ConcurrentHashMap<Location, Stargate> allGateBlocks = new ConcurrentHashMap<>();
    
    // List of All stargates indexed by name. Useful for dialing and such
    private static final ConcurrentHashMap<String, Stargate> stargateList = new ConcurrentHashMap<>();
    
    // List of stargates built but not named. Indexed by the player that built it.
    private static final ConcurrentHashMap<Player, Stargate> incompleteStargates = new ConcurrentHashMap<>();
    
    // List of stargates that have been activated but not yet dialed. Only used for gates without public use sign.
    private static final ConcurrentHashMap<Player, Stargate> activatedStargates = new ConcurrentHashMap<>();
    
    // List of networks indexed by their name
    private static final ConcurrentHashMap<String, StargateNetwork> stargateNetworks = new ConcurrentHashMap<>();
    
    // List of players ready to build a stargate, with the shape they are trying to build.
    private static final ConcurrentHashMap<Player, StargateShape> playerBuilders = new ConcurrentHashMap<>();

    // List of blocks that are part of an active animation. Only use this to make sure water doesn't flow everywhere.
    private static final ConcurrentHashMap<Location, Block> openingAnimationBlocks = new ConcurrentHashMap<>();
    
    // Repository for database operations
    private static StargateRepository stargateRepository;

    /**
     * Initializes the StargateManager with the plugin instance.
     * 
     * @param plugin The WormholeXTreme plugin instance
     */
    public static void initialize(WormholeXTreme plugin) {
        stargateRepository = plugin.getStargateRepository();
        loadAllGates();
    }
    
    /**
     * Loads all stargates from the database into memory.
     */
    private static void loadAllGates() {
        try {
            // Clear existing data
            allGateBlocks.clear();
            stargateList.clear();
            incompleteStargates.clear();
            activatedStargates.clear();
            
            // Load all stargates from the database
            List<Stargate> stargates = stargateRepository.findAll();
            
            // Add each stargate to memory
            for (Stargate stargate : stargates) {
                addStargateToMemory(stargate);
            }
            
            WormholeXTreme.getLog().info("Loaded " + stargates.size() + " stargates from the database");
        } catch (Exception e) {
            WormholeXTreme.getLog().log(Level.SEVERE, "Failed to load stargates from database", e);
        }
    }
    
    /**
     * Adds a stargate to the in-memory caches.
     * 
     * @param stargate The stargate to add
     */
    private static void addStargateToMemory(Stargate stargate) {
        // Add to stargate list
        stargateList.put(stargate.getGateName().toLowerCase(), stargate);
        
        // Add block indices
        for (Block block : stargate.getGateBlocks()) {
            allGateBlocks.put(block.getLocation(), stargate);
        }
        
        // Add to network if applicable
        if (stargate.getGateNetwork() != null) {
            StargateNetwork network = stargateNetworks.computeIfAbsent(
                stargate.getGateNetwork().getNetworkName().toLowerCase(), 
                k -> new StargateNetwork(stargate.getGateNetwork().getNetworkName())
            );
            network.getNetworkGateList().add(stargate);
        }
    }
    
    /**
     * This method adds a stargate that has been activated but not dialed by a player.
     * 
     * @param player The player who has activated the gate
     * @param stargate The gate the player has activated
     */
    public static void addActivatedStargate(Player player, Stargate stargate) {
        activatedStargates.put(player, stargate);
    }

    /**
     * This method adds an index mapping block location to stargate.
     * NOTE: This method does not verify that the block is part of the gate,
     * so it may not persist and won't be removed by removing the stargate. This can cause a gate to stay in memory!!!
     * 
     * @param block The block to index
     * @param stargate The stargate the block belongs to
     */
    public static void addBlockIndex(Block block, Stargate stargate) {
        if (block != null && stargate != null) {
            allGateBlocks.put(block.getLocation(), stargate);
        }
    }

    /**
     * Adds the gate to network.
     * 
     * @param gate The gate to add
     * @param network The network to add the gate to
     */
    public static void addGateToNetwork(Stargate gate, String network) {
        if (!getStargateNetworks().containsKey(network)) {
            addStargateNetwork(network);
        }

        StargateNetwork net;
        if ((net = getStargateNetworks().get(network)) != null) {
            synchronized (net.getNetworkGateLock()) {
                net.getNetworkGateList().add(gate);
                if (gate.isGateSignPowered()) {
                    net.getNetworkSignGateList().add(gate);
                }
            }
        }
    }

    /**
     * Adds a gate indexed by the player that hasn't yet been named and completed.
     * 
     * @param player The player
     * @param stargate The Stargate
     */
    public static void addIncompleteStargate(Player player, Stargate stargate) {
        getIncompleteStargates().put(player, stargate);
    }

    /**
     * Adds the player builder shape.
     * 
     * @param player The player
     * @param shape The shape
     */
    public static void addPlayerBuilderShape(Player player, StargateShape shape) {
        getPlayerBuilders().put(player, shape);
    }

    /**
     * Adds the given stargate to the list of stargates and saves it to the database.
     * 
     * @param stargate The stargate to add
     * @return true if the stargate was added successfully, false otherwise
     */
    public static boolean addStargate(Stargate stargate) {
        if (stargate == null) {
            return false;
        }
        
        try {
            // Save to database
            boolean success = stargateRepository.saveStargate(stargate);
            
            if (success) {
                // Add to memory
                addStargateToMemory(stargate);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            WormholeXTreme.getLog().log(Level.SEVERE, "Failed to save stargate to database", e);
            return false;
        }
    }

    /**
     * This method adds a stargate to the list of all stargates and saves it to the database.
     * 
     * @param stargate The stargate to add
     * @return true if the stargate was added successfully, false otherwise
     */
    public static boolean addStargate(Stargate stargate) {
        if (stargate == null) {
            return false;
        }
        
        try {
            // Save to database
            boolean success = stargateRepository.saveStargate(stargate);
            
            if (success) {
                // Add to memory
                addStargateToMemory(stargate);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            WormholeXTreme.getLog().log(Level.SEVERE, "Failed to save stargate to database", e);
            return false;
        }
    }

    /**
     * Complete stargate.
     * 
     * @param player The player
     * @param stargate The stargate
     * @return true, if successful
     */
    public static boolean completeStargate(Player player, Stargate stargate) {
        final Stargate posDupe = StargateManager.getStargate(stargate.getGateName());
        if (posDupe == null) {
            stargate.setGateOwner(player.getName());
            stargate.completeGate(stargate.getGateName(), "");
            WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, "Player: " + player.getName() + " completed a wormhole: " + stargate.getGateName());
            addStargate(stargate);
            return true;
        }

        return false;
    }

    /**
     * Complete stargate.
     * 
     * @param player The player
     * @param name The name
     * @param idc The idc
     * @param network The network
     * @return true, if successful
     */
    public static boolean completeStargate(Player player, String name, String idc, String network) {
        final Stargate complete = getIncompleteStargates().remove(player);

        if (complete != null) {
            if (!network.equals("")) {
                StargateNetwork net = StargateManager.getStargateNetwork(network);
                if (net == null) {
                    net = StargateManager.addStargateNetwork(network);
                }
                StargateManager.addGateToNetwork(complete, network);
                complete.setGateNetwork(net);
            }

            complete.setGateOwner(player.getName());
            complete.completeGate(name, idc);
            WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, "Player: " + player.getName() + " completed a wormhole: " + complete.getGateName());
            addStargate(complete);
            return true;
        }

        return false;
    }

    /**
     * Distance to closest stargate block.
     * 
     * @param self Location of the local object.
     * @param stargate Stargate to check blocks for distance.
     * @return square of distance to the closest stargate block.
     */
    public static double distanceSquaredToClosestGateBlock(Location self, Stargate stargate) {
        double distance = Double.MAX_VALUE;
        if ((stargate != null) && (self != null)) {
            final ArrayList<Location> gateblocks = stargate.getGateStructureBlocks();
            for (final Location l : gateblocks) {
                final double blockdistance = getSquaredDistance(self, l);
                if (blockdistance < distance) {
                    distance = blockdistance;
                }
            }
        }
        return distance;
    }

    /**
     * Find the closest stargate.
     * 
     * @param self Location of the local object.
     * @return The closest stargate to the local object.
     */
    public static Stargate findClosestStargate(Location self) {
        Stargate stargate = null;
        if (self != null) {
            final ArrayList<Stargate> gates = StargateManager.getAllGates();
            double man = Double.MAX_VALUE;
            for (final Stargate s : gates) {
                final Location t = s.getGatePlayerTeleportLocation();
                final double distance = getSquaredDistance(self, t);
                if (distance < man) {
                    man = distance;
                    stargate = s;
                }
            }
        }
        return stargate;
    }

    /**
     * Gets the activated stargates.
     * 
     * @return An unmodifiable map of activated stargates by player
     */
    public static Map<Player, Stargate> getActivatedStargates() {
        return Collections.unmodifiableMap(activatedStargates);
    }

    /**
     * Gets all gate blocks.
     * 
     * @return An unmodifiable map of all gate blocks
     */
    public static Map<Location, Stargate> getAllGateBlocks() {
        return Collections.unmodifiableMap(allGateBlocks);
    }

    /**
     * Get all gates.
     * This is more expensive than some other methods so it probably shouldn't be called a lot.
     * 
     * @return the array list
     */
    public static ArrayList<Stargate> getAllGates() {
        final ArrayList<Stargate> gates = new ArrayList<Stargate>();

        final Enumeration<Stargate> keys = getStargateList().elements();

        while (keys.hasMoreElements()) {
            gates.add(keys.nextElement());
        }

        return gates;
    }

    /**
     * Gets the gate from block.
     * 
     * @param block The block to get the gate from
     * @return the gate from block
     */
    public static Stargate getGateFromBlock(Block block) {
        if (getAllGateBlocks().containsKey(block.getLocation())) {
            return getAllGateBlocks().get(block.getLocation());
        }

        return null;
    }

    /**
     * Gets the incomplete stargates.
     * 
     * @return An unmodifiable map of incomplete stargates by player
     */
    public static Map<Player, Stargate> getIncompleteStargates() {
        return Collections.unmodifiableMap(incompleteStargates);
    }

    /**
     * Gets the opening animation blocks.
     * 
     * @return An unmodifiable map of opening animation blocks by location
     */
    public static Map<Location, Block> getOpeningAnimationBlocks() {
        return Collections.unmodifiableMap(openingAnimationBlocks);
    }

    /**
     * Gets the player builders.
     * 
     * @return An unmodifiable map of player builders by player
     */
    public static Map<Player, StargateShape> getPlayerBuilders() {
        return Collections.unmodifiableMap(playerBuilders);
    }

    /**
     * Gets the player builder shape.
     * 
     * @param player The player
     * @return the stargate shape
     */
    public static StargateShape getPlayerBuilderShape(Player player) {
        if (getPlayerBuilders().containsKey(player)) {
            return getPlayerBuilders().remove(player);
        }
        else {
            return null;
        }
    }

    /**
     * Gets the square of the distance between self and target
     * which saves the costly call to {@link Math#sqrt(double)}.
     * 
     * @param self Location of the local object.
     * @param target Location of the target object.
     * @return square of distance to target object from local object.
     */
    private static double getSquaredDistance(Location self, Location target) {
        double distance = Double.MAX_VALUE;
        if ((self != null) && (target != null)) {
            distance = Math.pow(self.getX() - target.getX(), 2) + Math.pow(self.getY() - target.getY(), 2) + Math.pow(self.getZ() - target.getZ(), 2);
        }
        return distance;
    }

    /**
     * Gets a stargate based on the name passed in. Returns null if there is no gate by that name.
     * 
     * @param name String name of the Stargate you want returned.
     * @return Stargate requested. Null if no stargate by that name.
     */
    public static Stargate getStargate(String name) {
        if (name == null) {
            return null;
        }
        
        // First try to get from memory
        Stargate stargate = stargateList.get(name.toLowerCase());
        
        // If not found in memory, try to load from database
        if (stargate == null) {
            try {
                stargate = stargateRepository.findByName(name).orElse(null);
                
                // If found in database, add to memory
                if (stargate != null) {
                    addStargateToMemory(stargate);
                }
            } catch (Exception e) {
                WormholeXTreme.getLog().log(Level.SEVERE, "Error loading stargate from database: " + name, e);
            }
        }
        
        return stargate;
    }

    /**
     * Gets the stargate list.
     * 
     * @return An unmodifiable map of all stargates by name
     */
    public static Map<String, Stargate> getStargateList() {
        return Collections.unmodifiableMap(stargateList);
    }

    /**
     * Gets the stargate network.
     * 
     * @param name The name
     * @return the stargate network
     */
    public static StargateNetwork getStargateNetwork(String name) {
        if (getStargateNetworks().containsKey(name)) {
            return getStargateNetworks().get(name);
        }
        else {
            return null;
        }
    }

    /**
     * Gets the stargate networks.
     * 
     * @return An unmodifiable map of stargate networks by name
     */
    public static Map<String, StargateNetwork> getStargateNetworks() {
        return Collections.unmodifiableMap(stargateNetworks);
    }

    // If block is a "gate" block this returns true.
    // This is useful to stop damage from being applied from an underpriveledged user.
    // Also used to stop flow of water, and prevent portal physics
    /**
     * Checks if is block in gate.
     * 
     * @param block The block to check
     * @return true, if is block in gate
     */
    public static boolean isBlockInGate(Block block) {
        return getAllGateBlocks().containsKey(block.getLocation()) || getOpeningAnimationBlocks().containsKey(block.getLocation());
    }

    /**
     * Checks if is stargate.
     * 
     * @param name The name
     * @return true, if is stargate
     */
    public static boolean isStargate(String name) {
        return getStargateList().containsKey(name);
    }

    /**
     * Returns the stargate that has been activated by that player.
     * Returns null if that player has not activated a gate.
     * 
     * @param player The player
     * @return Stargate that the player has activated. Null if no active gate.
     */
    public static Stargate removeActivatedStargate(Player player) {
        final Stargate s = getActivatedStargates().remove(player);
        //	if ( s != null )
        //		s.DeActivateStargate();
        return s;
    }

    /**
     * This method removes an index mapping block location to stargate.
     * NOTE: This method does not verify that the block has actually been removed from a gate
     * so it may not persist and can be readded when server is restarted.
     * 
     * @param block The block to remove
     */
    public static void removeBlockIndex(Block block) {
        if (block != null) {
            getAllGateBlocks().remove(block.getLocation());
        }
    }

    /**
     * Removes an incomplete stargate from the list.
     * 
     * @param player The player who created the gate.
     */
    public static void removeIncompleteStargate(Player player) {
        getIncompleteStargates().remove(player);
    }

    /**
     * This method removes a stargate from the list of all stargates and deletes it from the database.
     * 
     * @param stargate The stargate to remove
     * @return true if the stargate was removed successfully, false otherwise
     */
    public static boolean removeStargate(Stargate stargate) {
        if (stargate == null) {
            return false;
        }
        
        try {
            // Remove from database
            boolean success = stargateRepository.deleteStargate(stargate);
            
            if (success) {
                // Remove from memory
                stargateList.remove(stargate.getGateName().toLowerCase());
                
                // Remove block indices
                for (Block block : stargate.getGateBlocks()) {
                    allGateBlocks.remove(block.getLocation());
                {
                    s.getGateNetwork().getNetworkSignGateList().remove(s);
                }

                for (final Stargate s2 : s.getGateNetwork().getNetworkSignGateList())
                {
                    if ((s2.getGateDialSignTarget() != null) && (s2.getGateDialSignTarget().getGateId() == s.getGateId()) && s2.isGateSignPowered())
                    {
                        s2.setGateDialSignTarget(null);
                        if (s.getGateNetwork().getNetworkSignGateList().size() > 1)
                        {
                            s2.setGateDialSignIndex(0);
                            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(s2, ActionToTake.DIAL_SIGN_CLICK));
                            // s2.teleportSignClicked();
                        }
                    }
                }
            }
        }

        for (final Location b : s.getGateStructureBlocks())
        {
            getAllGateBlocks().remove(b);
        }

        for (final Location b : s.getGatePortalBlocks())
        {
            getAllGateBlocks().remove(b);
        }
    }

}