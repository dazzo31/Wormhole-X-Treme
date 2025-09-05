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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;

import com.wormhole_xtreme.wormhole.WormholeXTreme;
import com.wormhole_xtreme.wormhole.config.ConfigManager;
import com.wormhole_xtreme.wormhole.logic.StargateHelper;
import com.wormhole_xtreme.wormhole.permissions.PermissionsManager.PermissionLevel;

/**
 * WormholeXtreme StargateDBManager.
 * 
 * @author Ben Echols (Lologarithm)
 */
public class StargateDBManager
{

    /** The sql_connection. */
    private static Connection wormholeSQLConnection = null;

    /** The Store statement. */
    private static volatile PreparedStatement storeStatement;

    /** The Update gate statement. */
    private static volatile PreparedStatement updateGateStatement;

    /** The Get gate statement. */
    private static volatile PreparedStatement getGateStatement;

    /** The Remove statement. */
    private static volatile PreparedStatement removeStatement;

    /** The Update indv perm statement. */
    private static volatile PreparedStatement updateIndvPermStatement = null;

    /** The Store indv perm statement. */
    private static volatile PreparedStatement storeIndvPermStatement = null;

    /** The Get indv perm statement. */
    private static volatile PreparedStatement getIndvPermStatement = null;

    /** The Get all indv perm statement. */
    private static volatile PreparedStatement getAllIndvPermStatement = null;

    /**
     * Connect db.
     */
    private static void connectDB()
    {
        try
        {
            Class.forName("org.hsqldb.jdbcDriver");
        }
        catch (final Exception e)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "ERROR: failed to load HSQLDB JDBC driver.");
            e.printStackTrace();
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Failed to intialized internal DB. Stargates will not be saved: " + e.getMessage());
            return;
        }
        try {
            if ((wormholeSQLConnection == null) || wormholeSQLConnection.isClosed()) {
                setWormholeSQLConnection(DriverManager.getConnection("jdbc:hsqldb:./plugins/WormholeXTreme/WormholeXTremeDB/WormholeXTremeDB", "sa", ""));
                wormholeSQLConnection.setAutoCommit(true);
            } else {
                WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "WormholeDB already connected.");
            }
        } catch (SQLException ex) {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Failed to intialized internal DB. Stargates will not be saved: " + ex.getMessage());
        }
    }

    /**
     * Gets the all individual permissions.
     * 
     * @return the concurrent hash map
     */
    public static ConcurrentHashMap<String, PermissionLevel> getAllIndividualPermissions()
    {
        final ConcurrentHashMap<String, PermissionLevel> perms = new ConcurrentHashMap<String, PermissionLevel>();
        if (wormholeSQLConnection == null)
        {
            connectDB();
        }
        ResultSet perm = null;
        try
        {
            if (wormholeSQLConnection.isClosed())
            {
                connectDB();
            }

            if (getAllIndvPermStatement == null)
            {
                getAllIndvPermStatement = wormholeSQLConnection.prepareStatement("SELECT PlayerName, Permission FROM StargateIndividualPermissions;");
            }

            perm = getAllIndvPermStatement.executeQuery();
            while (perm.next())
            {
                perms.put(perm.getString("PlayerName"), PermissionLevel.valueOf(perm.getString("Permission")));
            }
        }
        catch (final SQLException e)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Error GetAllIndividualPermissions: " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (perm != null) {
                    perm.close();
                }
            }
            catch (final SQLException e)
            {
                WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, e.getMessage());
            }
        }
        return perms;
    }

    /**
     * Load stargates.
     * 
     * @param server
     *            the server
     */
    public static void loadStargates(final Server server)
    {
        if (wormholeSQLConnection == null)
        {
            connectDB();
        }
        final List<World> worlds = server.getWorlds();
        PreparedStatement stmt = null;
        ResultSet gatesData = null;
        try
        {
            if (wormholeSQLConnection.isClosed())
            {
                connectDB();
            }
            stmt = wormholeSQLConnection.prepareStatement("SELECT * FROM Stargates;");

            gatesData = stmt.executeQuery();
            while (gatesData.next())
            {
                final String networkName = gatesData.getString("Network");
                StargateNetwork sn = null;
                if (networkName != null)
                {
                    sn = StargateManager.getStargateNetwork(networkName);
                    if ((sn == null) && !networkName.equals(""))
                    {
                        sn = StargateManager.addStargateNetwork(networkName);
                    }
                }
                // Is this the best way to retrieve a world?
                // Legacy stored numeric world id (ignored in modern Bukkit API).
                final String worldName = gatesData.getString("WorldName");
                final String worldEnvironment = gatesData.getString("WorldEnvironment");

                World w = null;
                if (!worldName.equals("")) {
                    w = server.getWorld(worldName);
                }

                if ((w == null) && !worldName.equals(""))
                {
                    if (ConfigManager.isWormholeWorldsSupportEnabled())
                    {
                        if ((WormholeXTreme.getWorldHandler() != null) && !WormholeXTreme.getWorldHandler().loadWorld(worldName)) {
                            WorldCreator wc = WorldCreator.name(worldName).environment(Environment.valueOf(worldEnvironment));
                            server.createWorld(wc);
                            WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, true, "World: " + worldName + " is not a Wormhole World, the suggested action is to add it as one. Otherwise disregard this warning.");
                        }
                    }
                    else
                    {
                        WorldCreator wc = WorldCreator.name(worldName).environment(Environment.valueOf(worldEnvironment));
                        server.createWorld(wc);
                    }
                    w = server.getWorld(worldName);
                }
                else if (w == null)
                {
                    // Default to first world
                    w = worlds.get(0);
                }

                final Stargate s = StargateHelper.parseVersionedData(gatesData.getBytes("GateData"), w, gatesData.getString("Name"), sn);
                if (s != null)
                {
                    s.setGateId(gatesData.getInt("Id"));
                    s.setGateOwner(gatesData.getString("Owner"));
                    String gateShapeName = gatesData.getString("GateShape");
                    if (gateShapeName == null)
                    {
                        gateShapeName = "Standard";
                    }

                    s.setGateShape(StargateHelper.getStargateShape(gateShapeName));
                    if (sn != null)
                    {
                        sn.getNetworkGateList().add(s);
                        if (s.isGateSignPowered())
                        {
                            sn.getNetworkSignGateList().add(s);
                        }
                    }
                    StargateManager.addStargate(s);
                }
                else
                {
                    WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, true, "Failed to load Stargate '" + sn + "' from DB.");
                }
            }
            if (gatesData != null) { try { gatesData.close(); } catch (SQLException ignored) {} }
            if (stmt != null) { try { stmt.close(); } catch (SQLException ignored) {} }

            final ArrayList<Stargate> gateList = StargateManager.getAllGates();
            for (final Stargate s : gateList)
            {

                if (s.isGateLightsActive() && !s.isGateActive())
                {
                    s.lightStargate(false);
                }

                if (s.getGateTempTargetId() >= 0)
                {
                    // I know this is bad, I am just trying to get this feature out asap.
                    for (final Stargate t : gateList)
                    {
                        if (t.getGateId() == s.getGateTempTargetId())
                        {
                            s.dialStargate(t, true);
                            break;
                        }
                    }
                }

                if (s.getGateTempSignTarget() >= 0)
                {
                    // I know this is bad, I am just trying to get this feature out asap.
                    for (final Stargate t : gateList)
                    {
                        if (t.getGateId() == s.getGateTempSignTarget())
                        {
                            s.setGateDialSignTarget(t);
                            break;
                        }
                    }
                }
            }

            WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, gateList.size() + " Wormholes loaded from WormholeDB.");

        }
        catch (final SQLException e)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Error loading stargates from DB: " + e.getMessage());
        }
        finally {
            if (gatesData != null) { try { gatesData.close(); } catch (SQLException ignore) {} }
            if (stmt != null) { try { stmt.close(); } catch (SQLException ignore) {} }
        }
    }

    /**
     * Removes the stargate from sql.
     * 
     * @param s
     *            the s
     */
    protected static void removeStargateFromSQL(final Stargate s)
    {
        if (wormholeSQLConnection == null)
        {
            connectDB();
        }

        try
        {
            if (wormholeSQLConnection.isClosed())
            {
                connectDB();
            }
            if (removeStatement == null)
            {
                removeStatement = wormholeSQLConnection.prepareStatement("DELETE FROM Stargates WHERE name = ?;");
            }

            removeStatement.setString(1, s.getGateName());
            removeStatement.executeUpdate();
        }
        catch (final SQLException e)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Error storing stargate to DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets the wormhole sql connection.
     * 
     * @param connection
     *            the new wormhole sql connection
     */
    private static void setWormholeSQLConnection(final Connection connection)
    {
        StargateDBManager.wormholeSQLConnection = connection;
    }

    /**
     * Shutdown.
     */
    public static void shutdown()
    {
        try
        {
            if ( !wormholeSQLConnection.isClosed())
            {
                storeStatement = wormholeSQLConnection.prepareStatement("SHUTDOWN");
                storeStatement.execute();
                wormholeSQLConnection.close();
                WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, "WormholeDB shutdown successfull.");
            }
        }
        catch (final SQLException e)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, " Failed to shutdown:" + e.getMessage());
        }
    }

    /**
     * Stargate to sql.
     * 
     * @param s
     *            the s
     */
    public static void stargateToSQL(final Stargate s)
    {
        if (wormholeSQLConnection == null)
        {
            connectDB();
        }
        ResultSet gatesData = null;
        try
        {
            if (wormholeSQLConnection.isClosed())
            {
                connectDB();
            }
            if (getGateStatement == null)
            {
                getGateStatement = wormholeSQLConnection.prepareStatement("SELECT * FROM Stargates WHERE Name = ?");
            }
            getGateStatement.setString(1, s.getGateName());

            gatesData = getGateStatement.executeQuery();
            if (gatesData.next())
            {
                if (updateGateStatement == null)
                {
                    updateGateStatement = wormholeSQLConnection.prepareStatement("UPDATE Stargates SET GateData = ?, Network = ?, World = ?, WorldName = ?, WorldEnvironment = ?, Owner = ?, GateShape = ? WHERE Id = ?");
                }

                updateGateStatement.setBytes(1, StargateHelper.stargatetoBinary(s));
                if (s.getGateNetwork() != null)
                {
                    updateGateStatement.setString(2, s.getGateNetwork().getNetworkName());
                }
                else
                {
                    updateGateStatement.setString(2, "");
                }
                // World numeric ID removed; store ordinal placeholder (0) for backward compatibility.
                updateGateStatement.setLong(3, 0L);
                updateGateStatement.setString(4, s.getGateWorld().getName());
                updateGateStatement.setString(5, s.getGateWorld().getEnvironment().toString());
                updateGateStatement.setString(6, s.getGateOwner());
                if (s.getGateShape() == null)
                {
                    updateGateStatement.setString(7, "Standard");
                }
                else
                {
                    updateGateStatement.setString(7, s.getGateShape().getShapeName());
                }

                updateGateStatement.setLong(8, s.getGateId());
                updateGateStatement.executeUpdate();
            }
            else
            {
                if (gatesData != null) { try { gatesData.close(); } catch (SQLException ignore) {} }

                if (storeStatement == null)
                {
                    storeStatement = wormholeSQLConnection.prepareStatement("INSERT INTO Stargates(Name, GateData, Network, World, WorldName, WorldEnvironment, Owner, GateShape) VALUES ( ? , ? , ? , ? , ? , ?, ?, ? );");
                }

                storeStatement.setString(1, s.getGateName());
                final byte[] data = StargateHelper.stargatetoBinary(s);
                storeStatement.setBytes(2, data);
                if (s.getGateNetwork() != null)
                {
                    storeStatement.setString(3, s.getGateNetwork().getNetworkName());
                }
                else
                {
                    storeStatement.setString(3, "");
                }

                storeStatement.setLong(4, 0L);
                storeStatement.setString(5, s.getGateWorld().getName());
                storeStatement.setString(6, s.getGateWorld().getEnvironment().toString());
                storeStatement.setString(7, s.getGateOwner());
                storeStatement.setString(8, s.getGateShape().getShapeName());

                storeStatement.executeUpdate();

                getGateStatement.setString(1, s.getGateName());
                gatesData = getGateStatement.executeQuery();
                if (gatesData.next())
                {
                    s.setGateId(gatesData.getInt("Id"));
                }
            }
        }
        catch (final SQLException e)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Error storing stargate to DB: " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            if (gatesData != null) { try { gatesData.close(); } catch (SQLException ignore) {} }
        }
    }

    /**
     * Store individual permission in db.
     * 
     * @param player
     *            the player
     * @param pl
     *            the pl
     */
    public static void storeIndividualPermissionInDB(final String player, final PermissionLevel pl)
    {
        if (wormholeSQLConnection == null)
        {
            connectDB();
        }
        ResultSet perm = null;
        try
        {
            if (wormholeSQLConnection.isClosed())
            {
                connectDB();
            }

            if (getIndvPermStatement == null)
            {
                getIndvPermStatement = wormholeSQLConnection.prepareStatement("SELECT Permission FROM StargateIndividualPermissions WHERE PlayerName = ?;");
            }

            getIndvPermStatement.setString(1, player);
            perm = getIndvPermStatement.executeQuery();
            if ( !perm.next())
            {
                if (storeIndvPermStatement == null)
                {
                    storeIndvPermStatement = wormholeSQLConnection.prepareStatement("INSERT INTO StargateIndividualPermissions ( PlayerName, Permission ) VALUES ( ? , ? );");
                }

                storeIndvPermStatement.setString(1, player);
                storeIndvPermStatement.setString(2, pl.toString());
                storeIndvPermStatement.executeUpdate();
            }
            else
            {
                if (updateIndvPermStatement == null)
                {
                    updateIndvPermStatement = wormholeSQLConnection.prepareStatement("UPDATE StargateIndividualPermissions SET Permission = ? WHERE PlayerName = ?;");
                }

                updateIndvPermStatement.setString(2, player);
                updateIndvPermStatement.setString(1, pl.toString());
                final int modified = updateIndvPermStatement.executeUpdate();

                if (modified != 1)
                {
                    WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Failed to update " + player + " permissions in DB.");
                }
            }
        }
        catch (final SQLException e)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Error StoreIndividualPermissionInDB : " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (perm != null) {
                    perm.close();
                }
            }
            catch (final SQLException e)
            {
                WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, e.getMessage());
            }
        }
    }

}
