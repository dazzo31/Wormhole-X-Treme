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
import com.wormhole_xtreme.wormhole.config.ConfigManager;
import com.wormhole_xtreme.wormhole.logic.StargateUpdateRunnable;
import com.wormhole_xtreme.wormhole.logic.StargateUpdateRunnable.ActionToTake;
import com.wormhole_xtreme.wormhole.utils.WorldUtils;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.data.type.Switch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * WormholeXtreme Stargate Class/Instance.
 * 
 * @author Ben Echols (Lologarithm)
 * @author Dean Bailey (alron)
 * 
 */
@SuppressWarnings({"deprecation"})
public class Stargate
{
    // Note: legacy numeric ID mapping removed; use Material enums and BlockData throughout.

    // Note: legacy ID setter removed; all code paths now use Material and BlockData APIs.

    private static void placeWallSign(Block block, BlockFace facing) {
        block.setType(Material.OAK_WALL_SIGN);
        org.bukkit.block.data.BlockData bd = block.getBlockData();
        if (bd instanceof WallSign) {
            WallSign ws = (WallSign) bd;
            // Only allow cardinal facings; default to SOUTH if invalid
            BlockFace f = facing;
            if (!(f == BlockFace.NORTH || f == BlockFace.SOUTH || f == BlockFace.EAST || f == BlockFace.WEST)) {
                f = BlockFace.SOUTH;
            }
            ws.setFacing(f);
            block.setBlockData(ws);
        }
    }

    private static void placeLever(Block block, BlockFace facing, boolean powered) {
        block.setType(Material.LEVER);
        org.bukkit.block.data.BlockData bd = block.getBlockData();
        if (bd instanceof Switch) {
            Switch lv = (Switch) bd;
            BlockFace f = facing;
            if (!(f == BlockFace.NORTH || f == BlockFace.SOUTH || f == BlockFace.EAST || f == BlockFace.WEST)) {
                f = BlockFace.SOUTH;
            }
            lv.setFacing(f);
            lv.setPowered(powered);
            block.setBlockData(lv);
        }
    }

    private static void setLeverPowered(Block block, boolean powered) {
        if (block.getType() == Material.LEVER) {
            org.bukkit.block.data.BlockData bd = block.getBlockData();
            if (bd instanceof Switch) {
                Switch lv = (Switch) bd;
                lv.setPowered(powered);
                block.setBlockData(lv);
            }
        }
    }

    // Schema version constant removed (unused in runtime). Persisted data uses modern fields.
    
    /** The Loaded version, used to determine what version of parser to use. */
    private byte loadedVersion = -1;

    /** The gate id. */
    private int gateId = -1;
    /** Name of this gate, used to index and target. */
    private String gateName = "";
    /** Name of person who made the gate. */
    private String gateOwner = null;
    /** Network gate is connected to. */
    private StargateNetwork gateNetwork;
    /**
     * The gateshape that this gate uses.
     * This affects woosh depth and later materials
     */
    private StargateShape gateShape;
    /** The world this stargate is associated with. */
    private World gateWorld;
    /** Is this stargate already active? Can be active remotely and have no target of its own. */
    private boolean gateActive = false;

    /** Has this stargate been recently active?. */
    private boolean gateRecentlyActive = false;
    /** The direction that the stargate faces. */
    private BlockFace gateFacing;

    /** Is the stargate already lit up?. */
    private boolean gateLightsActive = false;
    /** Is activated through sign destination?. */
    private boolean gateSignPowered;
    /** The gate redstone powered. */
    private boolean gateRedstonePowered;
    /** The stargate that is being targeted by this gate. */
    private Stargate gateTarget = null;
    /** The current target on the sign, only used if gateSignPowered is true. */
    private Stargate gateDialSignTarget;
    /** Temp target id to store when loading gates. */
    private long gateTempSignTarget = -1;
    /** The network index the sign is pointing at. */
    private int gateDialSignIndex = 0;
    /** The temporary target stargate id. */
    private long gateTempTargetId = -1;
    /** The Iris deactivation code. */
    private String gateIrisDeactivationCode = "";
    /** Is the iris Active?. */
    private boolean gateIrisActive = false;
    /** The iris default setting. */
    private boolean gateIrisDefaultActive = false;
    /** The Teleport sign, used for selection of stargate target. */
    private Sign gateDialSign;
    /** The location to teleport players to. */
    private Location gatePlayerTeleportLocation;
    /** The location to teleport minecarts to. */
    private Location gateMinecartTeleportLocation;
    /** Location of the Button/Lever that activates this gate. */
    private Block gateDialLeverBlock;
    /** Location of the Button/Lever that activates the iris. */
    private Block gateIrisLeverBlock;
    /** The Teleport sign block. */
    private Block gateDialSignBlock;
    /** Block that toggle the activation state of the gate if nearby redstone is activated. */
    private Block gateRedstoneDialActivationBlock;
    /** Block that will toggle sign target when redstone nearby is activated. */
    private Block gateRedstoneSignActivationBlock;
    /** The gate redstone gate activated block. */
    private Block gateRedstoneGateActivatedBlock;
    /** The Name block holder. Where we place the stargate name sign. */
    private Block gateNameBlockHolder;
    /** The gate activate scheduler task id. */
    private int gateActivateTaskId;
    /** The gate shutdown scheduler task id. */
    private int gateShutdownTaskId;
    /** The gate after shutdown scheduler task id. */
    private int gateAfterShutdownTaskId;
    /** The gate animation step 3d. */
    private int gateAnimationStep3D = 1;
    /** The gate animation step 2d. */
    private int gateAnimationStep2D = 0;
    /** The animation removing. */
    private boolean gateAnimationRemoving = false;
    /** The current_lighting_iteration. */
    private int gateLightingCurrentIteration = 0;
    /** List of all blocks contained in this stargate, including buttons and levers. */
    private final ArrayList<Location> gateStructureBlocks = new ArrayList<Location>();
    /** List of all blocks that that are part of the "portal". */
    private final ArrayList<Location> gatePortalBlocks = new ArrayList<Location>();
    /** List of all blocks that turn on when gate is active. */
    private final ArrayList<ArrayList<Location>> gateLightBlocks = new ArrayList<ArrayList<Location>>();
    /** List of all blocks that woosh in order when gate is active. */
    private final ArrayList<ArrayList<Location>> gateWooshBlocks = new ArrayList<ArrayList<Location>>();
    /** The Animated blocks. */
    private final ArrayList<Block> gateAnimatedBlocks = new ArrayList<Block>();
    /** The gate_order. */
    private final HashMap<Integer, Stargate> gateSignOrder = new HashMap<Integer, Stargate>();

    /** The gate custom. */
    private boolean gateCustom = false;
    /** The gate custom structure material. */
    private Material gateCustomStructureMaterial = null;
    /** The gate custom portal material. */
    private Material gateCustomPortalMaterial = null;
    /** The gate custom light material. */
    private Material gateCustomLightMaterial = null;
    /** The gate custom iris material. */
    private Material gateCustomIrisMaterial = null;
    /** The gate custom woosh ticks. */
    private int gateCustomWooshTicks = -1;
    /** The gate custom light ticks. */
    private int gateCustomLightTicks = -1;
    /** The gate custom woosh depth. */
    private int gateCustomWooshDepth = -1;
    /** The gate custom woosh depth squared. */
    private int gateCustomWooshDepthSquared = -1;

    /**
     * Instantiates a new stargate.
     */
    public Stargate()
    {

    }

    /**
     * Animate opening.
     */
    public void animateOpening()
    {
        final Material wooshMaterial = isGateCustom()
            ? getGateCustomPortalMaterial()
            : getGateShape() != null
                ? getGateShape().getShapePortalMaterial()
                : Material.WATER;
        final int wooshDepth = isGateCustom()
            ? getGateCustomWooshDepth()
            : getGateShape() != null
                ? getGateShape().getShapeWooshDepth()
                : 0;

        if ((getGateWooshBlocks() != null) && (getGateWooshBlocks().size() > 0))
        {
            final ArrayList<Location> wooshBlockStep = getGateWooshBlocks().get(getGateAnimationStep3D());
            if ( !isGateAnimationRemoving())
            {
                if (wooshBlockStep != null)
                {
                    for (final Location l : wooshBlockStep)
                    {
                        final Block b = getGateWorld().getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                        getGateAnimatedBlocks().add(b);
                        StargateManager.getOpeningAnimationBlocks().put(l, b);
                        b.setType(wooshMaterial);
                    }

                    WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, getGateName() + " Woosh Adding: " + getGateAnimationStep3D() + " Woosh Block Size: " + wooshBlockStep.size());
                }

                if (getGateWooshBlocks().size() == getGateAnimationStep3D() + 1)
                {
                    setGateAnimationRemoving(true);
                }
                else
                {
                    setGateAnimationStep3D(getGateAnimationStep3D() + 1);
                }
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.ANIMATE_WOOSH), isGateCustom()
                    ? getGateCustomWooshTicks()
                    : getGateShape() != null
                        ? getGateShape().getShapeWooshTicks()
                        : 2);
            }
            else
            {
                // remove in reverse order, if block is not a portal block!
                if (wooshBlockStep != null)
                {
                    for (final Location l : wooshBlockStep)
                    {
                        final Block b = getGateWorld().getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                        StargateManager.getOpeningAnimationBlocks().remove(l, b);
                        getGateAnimatedBlocks().remove(b);
                        if ( !StargateManager.isBlockInGate(b))
                        {
                            b.setType(Material.AIR);
                        }
                    }
                    WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, getGateName() + " Woosh Removing: " + getGateAnimationStep3D() + " Woosh Block Size: " + wooshBlockStep.size());
                }

                // If this is the last step to animate, we now add all the portal blocks in.
                if (getGateAnimationStep3D() == 1)
                {
                    setGateAnimationRemoving(false);
                    if (isGateLightsActive() && isGateActive())
                    {
                        fillGateInterior(wooshMaterial);
                    }
                }
                else
                {
                    setGateAnimationStep3D(getGateAnimationStep3D() - 1);
                    WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.ANIMATE_WOOSH), isGateCustom()
                        ? getGateCustomWooshTicks()
                        : getGateShape() != null
                            ? getGateShape().getShapeWooshTicks()
                            : 2);
                }
            }
        }
        else
        {
            if ((getGateAnimationStep2D() == 0) && (wooshDepth > 0))
            {
                for (final Location block : getGatePortalBlocks())
                {
                    final Block r = getGateWorld().getBlockAt(block.getBlockX(), block.getBlockY(), block.getBlockZ()).getRelative(getGateFacing());
                    r.setType(wooshMaterial);
                    getGateAnimatedBlocks().add(r);
                    StargateManager.getOpeningAnimationBlocks().put(r.getLocation(), r);
                }
                setGateAnimationStep2D(getGateAnimationStep2D() + 1);
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.ANIMATE_WOOSH), 4);
            }
            else if (getGateAnimationStep2D() < wooshDepth)
            {
                final int size = getGateAnimatedBlocks().size();
                final int start = getGatePortalBlocks().size();
                for (int i = (size - start); i < size; i++)
                {
                    final Block b = getGateAnimatedBlocks().get(i);
                    final Block r = b.getRelative(getGateFacing());
                    r.setType(wooshMaterial);
                    getGateAnimatedBlocks().add(r);
                    StargateManager.getOpeningAnimationBlocks().put(r.getLocation(), r);
                }
                setGateAnimationStep2D(getGateAnimationStep2D() + 1);
                if (getGateAnimationStep2D() == wooshDepth)
                {
                    WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.ANIMATE_WOOSH), 8);
                }
                else
                {
                    WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.ANIMATE_WOOSH), 4);
                }
            }
            else if (getGateAnimationStep2D() >= wooshDepth)
            {
                for (int i = 0; i < getGatePortalBlocks().size(); i++)
                {
                    final int index = getGateAnimatedBlocks().size() - 1;
                    if (index >= 0)
                    {
                        final Block b = getGateAnimatedBlocks().get(index);
                        b.setType(Material.AIR);
                        getGateAnimatedBlocks().remove(index);
                        StargateManager.getOpeningAnimationBlocks().remove(b.getLocation());
                    }
                }
                if (getGateAnimationStep2D() < ((wooshDepth * 2) - 1))
                {
                    setGateAnimationStep2D(getGateAnimationStep2D() + 1);
                    WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.ANIMATE_WOOSH), 3);
                }
                else
                {
                    setGateAnimationStep2D(0);
                    if (isGateActive())
                    {
                        fillGateInterior(wooshMaterial);
                    }
                }
            }
        }
    }

    /**
     * Complete gate.
     * 
     * @param name
     *            the name
     * @param idc
     *            the idc
     */
    void completeGate(final String name, final String idc)
    {
        setGateName(name);

        // 1. Setup Name Sign
        if (getGateNameBlockHolder() != null)
        {
            setupGateSign(true);
        }
        // 2. Set up Iris stuff
        setIrisDeactivationCode(idc);

        if (isGateRedstonePowered())
        {
            setupRedstoneGateActivatedLever(true);
            if (isGateSignPowered())
            {
                setupRedstoneDialWire(true);
                setupRedstoneSignDialWire(true);
            }
        }
    }

    /**
     * Delete gate blocks.
     */
    public void deleteGateBlocks()
    {
        for (final Location bc : getGateStructureBlocks())
        {
            final Block b = getGateWorld().getBlockAt(bc.getBlockX(), bc.getBlockY(), bc.getBlockZ());
            b.setType(Material.AIR);
        }
    }

    /**
     * Delete portal blocks.
     */
    public void deletePortalBlocks()
    {
        for (final Location bc : getGatePortalBlocks())
        {
            final Block b = getGateWorld().getBlockAt(bc.getBlockX(), bc.getBlockY(), bc.getBlockZ());
            b.setType(Material.AIR);
        }
    }

    /**
     * Delete teleport sign.
     */
    public void deleteTeleportSign()
    {
        if ((getGateDialSignBlock() != null) && (getGateDialSign() != null))
        {
            final Block teleportSign = getGateDialSignBlock().getRelative(getGateFacing());
            teleportSign.setType(Material.AIR);
        }
    }

    /**
     * This method activates the current stargate as if it had just been dialed.
     * This includes filling the event horizon, canceling any other shutdown events,
     * scheduling the shutdown time and scheduling the WOOSH if enabled.
     * Failed task schedules will cause gate to not activate, fill, or animate.
     */
    private void dialStargate()
    {
        WorldUtils.scheduleChunkLoad(getGatePlayerTeleportLocation().getBlock());
        if (getGateShutdownTaskId() > 0)
        {
            WormholeXTreme.getScheduler().cancelTask(getGateShutdownTaskId());
        }
        if (getGateAfterShutdownTaskId() > 0)
        {
            WormholeXTreme.getScheduler().cancelTask(getGateAfterShutdownTaskId());
        }

        final int timeout = ConfigManager.getTimeoutShutdown() * 20;
        if (timeout > 0)
        {
            setGateShutdownTaskId(WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.SHUTDOWN), timeout));
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ShutdownTaskID \"" + getGateShutdownTaskId() + "\" created.");
            if (getGateShutdownTaskId() == -1)
            {
                shutdownStargate(true);
                WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Failed to schdule wormhole shutdown timeout: " + timeout + " Received task id of -1. Wormhole forced closed NOW.");
            }
        }

        if ((getGateShutdownTaskId() > 0) || (timeout == 0))
        {
            if ( !isGateActive())
            {
                setGateActive(true);
                toggleDialLeverState(false);
                toggleRedstoneGateActivatedPower();
                setGateRecentlyActive(false);
            }
            if ( !isGateLightsActive())
            {
                // This function lights, wooshes, and then adds portal material
                lightStargate(true);
            }
            else
            {
                // Just skip top woosh if already lit (/dial gate)
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.ANIMATE_WOOSH));
            }
        }
        else
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "No wormhole. No visual events.");
        }
    }

    /**
     * This method takes in a remote stargate and dials it if it is not active.
     * 
     * @param target
     *            the target stargate
     * @param force
     *            true to force dial the stargate, false to properly check if target gate is not active.
     * @return True if successful, False if remote target is already Active or if there is a failure scheduling stargate
     *         shutdowns.
     */
    public boolean dialStargate(final Stargate target, final boolean force)
    {
        if (getGateActivateTaskId() > 0)
        {
            WormholeXTreme.getScheduler().cancelTask(getGateActivateTaskId());
        }

        if ( !target.isGateLightsActive() || force)
        {
            setGateTarget(target);
            dialStargate();
            getGateTarget().dialStargate();
            if ((isGateActive()) && (getGateTarget().isGateActive()))
            {
                return true;
            }
            else if ((isGateActive()) && ( !getGateTarget().isGateActive()))
            {
                shutdownStargate(true);
                WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "Far wormhole failed to open. Closing local wormhole for safety sake.");
            }
            else if (( !isGateActive()) && (getGateTarget().isGateActive()))
            {
                target.shutdownStargate(true);
                WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "Local wormhole failed to open. Closing far end wormhole for safety sake.");
            }
        }

        return false;
    }

    /**
     * Fill gate interior.
     * 
     * @param typeId
     *            the type id
     */
    public void fillGateInterior(final Material material)
    {
        for (final Location bc : getGatePortalBlocks())
        {
            final Block b = getGateWorld().getBlockAt(bc.getBlockX(), bc.getBlockY(), bc.getBlockZ());
            b.setType(material);
        }
    }

    /**
     * Gets the gate activate task id.
     * 
     * @return the gate activate task id
     */
    private int getGateActivateTaskId()
    {
        return gateActivateTaskId;
    }

    /**
     * Gets the gate after shutdown task id.
     * 
     * @return the gate after shutdown task id
     */
    private int getGateAfterShutdownTaskId()
    {
        return gateAfterShutdownTaskId;
    }

    /**
     * Gets the gate animated blocks.
     * 
     * @return the gate animated blocks
     */
    private ArrayList<Block> getGateAnimatedBlocks()
    {
        return gateAnimatedBlocks;
    }

    /**
     * Gets the gate animation step 2d.
     * 
     * @return the gate animation step 2d
     */
    public int getGateAnimationStep2D()
    {
        return gateAnimationStep2D;
    }

    /**
     * Gets the gate animation step.
     * 
     * @return the gate animation step
     */
    private int getGateAnimationStep3D()
    {
        return gateAnimationStep3D;
    }

    /**
     * Gets the gate custom iris material.
     * 
     * @return the gate custom iris material
     */
    public Material getGateCustomIrisMaterial()
    {
        return gateCustomIrisMaterial;
    }

    /**
     * Gets the gate custom light material.
     * 
     * @return the gate custom light material
     */
    public Material getGateCustomLightMaterial()
    {
        return gateCustomLightMaterial;
    }

    /**
     * Gets the gate custom light ticks.
     * 
     * @return the gate custom light ticks
     */
    public int getGateCustomLightTicks()
    {
        return gateCustomLightTicks;
    }

    /**
     * Gets the gate custom portal material.
     * 
     * @return the gate custom portal material
     */
    public Material getGateCustomPortalMaterial()
    {
        return gateCustomPortalMaterial;
    }

    /**
     * Gets the gate custom structure material.
     * 
     * @return the gate custom structure material
     */
    public Material getGateCustomStructureMaterial()
    {
        return gateCustomStructureMaterial;
    }

    /**
     * Gets the gate custom woosh depth.
     * 
     * @return the gate custom woosh depth
     */
    public int getGateCustomWooshDepth()
    {
        return gateCustomWooshDepth;
    }

    /**
     * Gets the gate custom woosh depth squared.
     * 
     * @return the gate custom woosh depth squared
     */
    public int getGateCustomWooshDepthSquared()
    {
        return gateCustomWooshDepthSquared;
    }

    /**
     * Gets the gate custom woosh ticks.
     * 
     * @return the gate custom woosh ticks
     */
    public int getGateCustomWooshTicks()
    {
        return gateCustomWooshTicks;
    }

    /**
     * Gets the gate activation block.
     * 
     * @return the gate activation block
     */
    public Block getGateDialLeverBlock()
    {
        return gateDialLeverBlock;
    }

    /**
     * Gets the gate teleport sign.
     * 
     * @return the gate teleport sign
     */
    public synchronized Sign getGateDialSign()
    {
        return gateDialSign;
    }

    /**
     * Gets the gate teleport sign block.
     * 
     * @return the gate teleport sign block
     */
    public synchronized Block getGateDialSignBlock()
    {
        return gateDialSignBlock;
    }

    /**
     * Gets the gate sign index.
     * 
     * @return the gate sign index
     */
    public synchronized int getGateDialSignIndex()
    {
        return gateDialSignIndex;
    }

    /**
     * Gets the gate sign target.
     * 
     * @return the gate sign target
     */
    public Stargate getGateDialSignTarget()
    {
        return gateDialSignTarget;
    }

    /**
     * Gets the gate facing.
     * 
     * @return the gate facing
     */
    public BlockFace getGateFacing()
    {
        return gateFacing;
    }

    /**
     * Gets the gate id.
     * 
     * @return the gate id
     */
    public int getGateId()
    {
        return gateId;
    }

    /**
     * Sets the gate id.
     * 
     * @param gateId the new gate id
     */
    public void setGateId(int gateId)
    {
        this.gateId = gateId;
    }

    /**
     * Gets the gate iris deactivation code.
     * 
     * @return the gate iris deactivation code
     */
    public String getGateIrisDeactivationCode()
    {
        return gateIrisDeactivationCode;
    }

    /**
     * Gets the gate iris activation block.
     * 
     * @return the gate iris activation block
     */
    public Block getGateIrisLeverBlock()
    {
        return gateIrisLeverBlock;
    }

    /**
     * Gets the gate light blocks.
     * 
     * @return the gate light blocks
     */
    public ArrayList<ArrayList<Location>> getGateLightBlocks()
    {
        return gateLightBlocks;
    }

    /**
     * Gets the gate lighting current iteration.
     * 
     * @return the gate lighting current iteration
     */
    private int getGateLightingCurrentIteration()
    {
        return gateLightingCurrentIteration;
    }

    /**
     * Gets the gate minecart teleport location.
     * 
     * @return the gate minecart teleport location
     */
    public Location getGateMinecartTeleportLocation()
    {
        return gateMinecartTeleportLocation;
    }

    /**
     * Gets the gate name.
     * 
     * @return the gate name
     */
    public String getGateName()
    {
        return gateName;
    }

    /**
     * Gets the gate name block holder.
     * 
     * @return the gate name block holder
     */
    public Block getGateNameBlockHolder()
    {
        return gateNameBlockHolder;
    }

    /**
     * Gets the gate network.
     * 
     * @return the gate network
     */
    public StargateNetwork getGateNetwork()
    {
        return gateNetwork;
    }

    /**
     * Gets the gate owner.
     * 
     * @return the gate owner
     */
    public String getGateOwner()
    {
        return gateOwner;
    }

    /**
     * Gets the gate teleport location.
     * 
     * @return the gate teleport location
     */
    public Location getGatePlayerTeleportLocation()
    {
        return gatePlayerTeleportLocation;
    }

    /**
     * Gets the gate portal blocks.
     * 
     * @return the gate portal blocks
     */
    public ArrayList<Location> getGatePortalBlocks()
    {
        return gatePortalBlocks;
    }

    /**
     * Gets the gate redstone activation block.
     * 
     * @return the gate redstone activation block
     */
    public Block getGateRedstoneDialActivationBlock()
    {
        return gateRedstoneDialActivationBlock;
    }

    /**
     * Gets the gate redstone gate activated block.
     * 
     * @return the gate redstone gate activated block
     */
    public Block getGateRedstoneGateActivatedBlock()
    {
        return gateRedstoneGateActivatedBlock;
    }

    /**
     * Gets the gate redstone dial change block.
     * 
     * @return the gate redstone dial change block
     */
    public Block getGateRedstoneSignActivationBlock()
    {
        return gateRedstoneSignActivationBlock;
    }

    /**
     * Gets the gate shape.
     * 
     * @return the gate shape
     */
    public StargateShape getGateShape()
    {
        return gateShape;
    }

    /**
     * Gets the gate shutdown task id.
     * 
     * @return the gate shutdown task id
     */
    private int getGateShutdownTaskId()
    {
        return gateShutdownTaskId;
    }

    /**
     * Gets the gate sign order.
     * 
     * @return the gate sign order
     */
    private HashMap<Integer, Stargate> getGateSignOrder()
    {
        return gateSignOrder;
    }

    /**
     * Gets the gate structure blocks.
     * 
     * @return the gate structure blocks
     */
    public ArrayList<Location> getGateStructureBlocks()
    {
        return gateStructureBlocks;
    }

    /**
     * Gets the gate target.
     * 
     * @return the gate target
     */
    public Stargate getGateTarget()
    {
        return gateTarget;
    }

    /**
     * Gets the gate temp sign target.
     * 
     * @return the gate temp sign target
     */
    long getGateTempSignTarget()
    {
        return gateTempSignTarget;
    }

    /**
     * Gets the gate temp target id.
     * 
     * @return the gate temp target id
     */
    long getGateTempTargetId()
    {
        return gateTempTargetId;
    }

    /**
     * Gets the gate woosh blocks.
     * 
     * @return the gate woosh blocks
     */
    public ArrayList<ArrayList<Location>> getGateWooshBlocks()
    {
        return gateWooshBlocks;
    }

    /**
     * Gets the gate world.
     * 
     * @return the gate world
     */
    public World getGateWorld()
    {
        return gateWorld;
    }

    /**
     * Gets the loaded version.
     * 
     * @return the loaded version
     */
    public byte getLoadedVersion()
    {
        return loadedVersion;
    }

    /**
     * Checks if is gate active.
     * 
     * @return true, if is gate active
     */
    public boolean isGateActive()
    {
        return gateActive;
    }

    /**
     * Checks if is gate animation removing.
     * 
     * @return true, if is gate animation removing
     */
    private boolean isGateAnimationRemoving()
    {
        return gateAnimationRemoving;
    }

    /**
     * Checks if is gate custom.
     * 
     * @return true, if is gate custom
     */
    public boolean isGateCustom()
    {
        return gateCustom;
    }

    /**
     * Checks if is gate iris active.
     * 
     * @return true, if is gate iris active
     */
    public boolean isGateIrisActive()
    {
        return gateIrisActive;
    }

    /**
     * Checks if is gate iris default active.
     * 
     * @return true, if is gate iris default active
     */
    private boolean isGateIrisDefaultActive()
    {
        return gateIrisDefaultActive;
    }

    /**
     * Checks if is gate lit.
     * 
     * @return true, if is gate lit
     */
    public boolean isGateLightsActive()
    {
        return gateLightsActive;
    }

    /**
     * Checks if is gate recently active.
     * 
     * @return true, if is gate recently active
     */
    public boolean isGateRecentlyActive()
    {
        return gateRecentlyActive;
    }

    /**
     * Checks if is gate redstone powered.
     * 
     * @return true, if is gate redstone powered
     */
    public boolean isGateRedstonePowered()
    {
        return gateRedstonePowered;
    }

    /**
     * Checks if is gate sign powered.
     * 
     * @return true, if is gate sign powered
     */
    public boolean isGateSignPowered()
    {
        return gateSignPowered;
    }

    /**
     * Light or darken stargate and kick off woosh animation on active stargates.
     * 
     * @param on
     *            true to light, false to darken.
     */
    public void lightStargate(final boolean on)
    {
        if (on)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Lighting up Order: " + getGateLightingCurrentIteration());
            if (getGateLightingCurrentIteration() == 0)
            {
                setGateLightsActive(true);
            }
            else if ( !isGateLightsActive())
            {
                lightStargate(false);
                setGateLightingCurrentIteration(0);
                return;
            }
            setGateLightingCurrentIteration(getGateLightingCurrentIteration() + 1);
            // Light up blocks
            if (getGateLightBlocks() != null)
            {
                if ((getGateLightBlocks().size() > 0) && (getGateLightBlocks().get(getGateLightingCurrentIteration()) != null))
                {
                    for (final Location l : getGateLightBlocks().get(getGateLightingCurrentIteration()))
                    {
                        final Block b = getGateWorld().getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                        b.setType(isGateCustom()
                            ? getGateCustomLightMaterial()
                            : getGateShape() != null
                                ? getGateShape().getShapeLightMaterial()
                                : Material.GLOWSTONE);
                    }
                }

                if (getGateLightingCurrentIteration() >= getGateLightBlocks().size() - 1)
                {
                    // Reset back to start
                    setGateLightingCurrentIteration(0);
                    if (isGateActive())
                    {
                        // Start up animation for woosh now!
                        WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.ANIMATE_WOOSH));
                    }
                }
                else
                {
                    // Keep lighting
                    WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.LIGHTUP), isGateCustom()
                        ? getGateCustomLightTicks()
                        : getGateShape() != null
                            ? getGateShape().getShapeLightTicks()
                            : 2);
                }
            }
        }
        else
        {
            setGateLightsActive(false);
            // Remove Light Up Blocks
            if (getGateLightBlocks() != null)
            {
                for (int i = 0; i < getGateLightBlocks().size(); i++)
                {
                    if (getGateLightBlocks().get(i) != null)
                    {
                        for (final Location l : getGateLightBlocks().get(i))
                        {
                            final Block b = getGateWorld().getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                            b.setType(isGateCustom()
                                ? getGateCustomStructureMaterial()
                                : getGateShape() != null
                                    ? getGateShape().getShapeStructureMaterial()
                                    : Material.OBSIDIAN);
                        }
                    }
                }
            }
        }
    }

    /**
     * Reset sign.
     * 
     * @param teleportSign
     *            the teleport sign
     */
    public void resetSign(final boolean teleportSign)
    {
        if (teleportSign)
        {
            placeWallSign(getGateDialSignBlock(), getGateFacing());
            setGateDialSign((Sign) getGateDialSignBlock().getState());
            getGateDialSign().setLine(0, getGateName());
            if (getGateNetwork() != null)
            {
                getGateDialSign().setLine(1, getGateNetwork().getNetworkName());
            }
            else
            {
                getGateDialSign().setLine(1, "");
            }
            getGateDialSign().setLine(2, "");
            getGateDialSign().setLine(3, "");
            getGateDialSign().update(true);
        }
    }

    /**
     * Reset teleport sign.
     */
    public void resetTeleportSign()
    {
        if ((getGateDialSignBlock() != null) && (getGateDialSign() != null))
        {
            getGateDialSignBlock().setType(Material.AIR);
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.DIAL_SIGN_RESET), 2);
        }
    }

    /**
     * Sets the gate activate task id.
     * 
     * @param gateActivateTaskId
     *            the new gate activate task id
     */
    private void setGateActivateTaskId(final int gateActivateTaskId)
    {
        this.gateActivateTaskId = gateActivateTaskId;
    }

    /**
     * Sets the gate active.
     * 
     * @param gateActive
     *            the new gate active
     */
    public void setGateActive(final boolean gateActive)
    {
        this.gateActive = gateActive;
    }

    /**
     * Sets the gate after shutdown task id.
     * 
     * @param gateAfterShutdownTaskId
     *            the new gate after shutdown task id
     */
    private void setGateAfterShutdownTaskId(final int gateAfterShutdownTaskId)
    {
        this.gateAfterShutdownTaskId = gateAfterShutdownTaskId;
    }

    /**
     * Sets the gate animation removing.
     * 
     * @param gateAnimationRemoving
     *            the new gate animation removing
     */
    private void setGateAnimationRemoving(final boolean gateAnimationRemoving)
    {
        this.gateAnimationRemoving = gateAnimationRemoving;
    }

    /**
     * Sets the gate animation step 2d.
     * 
     * @param gateAnimationStep2D
     *            the new gate animation step 2d
     */
    public void setGateAnimationStep2D(final int gateAnimationStep2D)
    {
        this.gateAnimationStep2D = gateAnimationStep2D;
    }

    /**
     * Sets the gate animation step.
     * 
     * @param gateAnimationStep
     *            the new gate animation step
     */
    private void setGateAnimationStep3D(final int gateAnimationStep3D)
    {
        this.gateAnimationStep3D = gateAnimationStep3D;
    }

    /**
     * Sets the gate custom.
     * 
     * @param gateCustom
     *            the new gate custom
     */
    public void setGateCustom(final boolean gateCustom)
    {
        this.gateCustom = gateCustom;
    }

    /**
     * Sets the gate custom iris material.
     * 
     * @param gateCustomIrisMaterial
     *            the new gate custom iris material
     */
    public void setGateCustomIrisMaterial(final Material gateCustomIrisMaterial)
    {
        this.gateCustomIrisMaterial = gateCustomIrisMaterial;
    }

    /**
     * Sets the gate custom light material.
     * 
     * @param gateCustomLightMaterial
     *            the new gate custom light material
     */
    public void setGateCustomLightMaterial(final Material gateCustomLightMaterial)
    {
        this.gateCustomLightMaterial = gateCustomLightMaterial;
    }

    /**
     * Sets the gate custom light ticks.
     * 
     * @param gateCustomLightTicks
     *            the new gate custom light ticks
     */
    public void setGateCustomLightTicks(final int gateCustomLightTicks)
    {
        this.gateCustomLightTicks = gateCustomLightTicks;
    }

    /**
     * Sets the gate custom portal material.
     * 
     * @param gateCustomPortalMaterial
     *            the new gate custom portal material
     */
    public void setGateCustomPortalMaterial(final Material gateCustomPortalMaterial)
    {
        this.gateCustomPortalMaterial = gateCustomPortalMaterial;
    }

    /**
     * Sets the gate custom structure material.
     * 
     * @param gateCustomStructureMaterial
     *            the new gate custom structure material
     */
    public void setGateCustomStructureMaterial(final Material gateCustomStructureMaterial)
    {
        this.gateCustomStructureMaterial = gateCustomStructureMaterial;
    }

    /**
     * Sets the gate custom woosh depth.
     * 
     * @param gateCustomWooshDepth
     *            the new gate custom woosh depth
     */
    public void setGateCustomWooshDepth(final int gateCustomWooshDepth)
    {
        this.gateCustomWooshDepth = gateCustomWooshDepth;
    }

    /**
     * Sets the gate custom woosh depth squared.
     * 
     * @param gateCustomWooshDepthSquared
     *            the new gate custom woosh depth squared
     */
    public void setGateCustomWooshDepthSquared(final int gateCustomWooshDepthSquared)
    {
        this.gateCustomWooshDepthSquared = gateCustomWooshDepthSquared;
    }

    /**
     * Sets the gate custom woosh ticks.
     * 
     * @param gateCustomWooshTicks
     *            the new gate custom woosh ticks
     */
    public void setGateCustomWooshTicks(final int gateCustomWooshTicks)
    {
        this.gateCustomWooshTicks = gateCustomWooshTicks;
    }

    /**
     * Sets the gate activation block.
     * 
     * @param gateDialLeverBlock
     *            the new gate dial lever block
     */
    public void setGateDialLeverBlock(final Block gateDialLeverBlock)
    {
        this.gateDialLeverBlock = gateDialLeverBlock;
    }

    /**
     * Sets the gate teleport sign.
     * 
     * @param gateDialSign
     *            the new gate dial sign
     */
    public synchronized void setGateDialSign(final Sign gateDialSign)
    {
        this.gateDialSign = gateDialSign;
    }

    /**
     * Sets the gate teleport sign block.
     * 
     * @param gateDialSignBlock
     *            the new gate dial sign block
     */
    public synchronized void setGateDialSignBlock(final Block gateDialSignBlock)
    {
        this.gateDialSignBlock = gateDialSignBlock;
    }

    /**
     * Sets the gate sign index.
     * 
     * @param gateDialSignIndex
     *            the new gate dial sign index
     */
    public synchronized void setGateDialSignIndex(final int gateDialSignIndex)
    {
        this.gateDialSignIndex = gateDialSignIndex;
    }

    /**
     * Sets the gate sign target.
     * 
     * @param gateDialSignTarget
     *            the new gate dial sign target
     */
    protected void setGateDialSignTarget(final Stargate gateDialSignTarget)
    {
        this.gateDialSignTarget = gateDialSignTarget;
    }

    /**
     * Sets the gate facing.
     * 
     * @param gateFacing
     *            the new gate facing
     */
    public void setGateFacing(final BlockFace gateFacing)
    {
        this.gateFacing = gateFacing;
    }

    /**
     * Sets the gate id.
     * 
     * @param gateId
     *            the new gate id
     */
    void setGateId(final long gateId)
    {
        this.gateId = (int) gateId;
    }

    /**
     * Sets the gate iris active.
     * 
     * @param gateIrisActive
     *            the new gate iris active
     */
    public void setGateIrisActive(final boolean gateIrisActive)
    {
        this.gateIrisActive = gateIrisActive;
    }

    /**
     * Sets the gate iris deactivation code.
     * 
     * @param gateIrisDeactivationCode
     *            the new gate iris deactivation code
     */
    public void setGateIrisDeactivationCode(final String gateIrisDeactivationCode)
    {
        this.gateIrisDeactivationCode = gateIrisDeactivationCode;
    }

    /**
     * Sets the gate iris default active.
     * 
     * @param gateIrisDefaultActive
     *            the new gate iris default active
     */
    public void setGateIrisDefaultActive(final boolean gateIrisDefaultActive)
    {
        this.gateIrisDefaultActive = gateIrisDefaultActive;
    }

    /**
     * Sets the gate iris activation block.
     * 
     * @param gateIrisLeverBlock
     *            the new gate iris lever block
     */
    public void setGateIrisLeverBlock(final Block gateIrisLeverBlock)
    {
        this.gateIrisLeverBlock = gateIrisLeverBlock;
    }

    /**
     * Sets the gate lighting current iteration.
     * 
     * @param gateLightingCurrentIteration
     *            the new gate lighting current iteration
     */
    private void setGateLightingCurrentIteration(final int gateLightingCurrentIteration)
    {
        this.gateLightingCurrentIteration = gateLightingCurrentIteration;
    }

    /**
     * Sets the gate lit.
     * 
     * @param gateLightsActive
     *            the new gate lights active
     */
    public void setGateLightsActive(final boolean gateLightsActive)
    {
        this.gateLightsActive = gateLightsActive;
    }

    /**
     * Sets the gate minecart teleport location.
     * 
     * @param gateMinecartTeleportLocation
     *            the new gate minecart teleport location
     */
    public void setGateMinecartTeleportLocation(final Location gateMinecartTeleportLocation)
    {
        this.gateMinecartTeleportLocation = gateMinecartTeleportLocation;
    }

    /**
     * Sets the gate name.
     * 
     * @param gateName
     *            the new gate name
     */
    public void setGateName(final String gateName)
    {
        this.gateName = gateName;
    }

    /**
     * Sets the gate name block holder.
     * 
     * @param gateNameBlockHolder
     *            the new gate name block holder
     */
    public void setGateNameBlockHolder(final Block gateNameBlockHolder)
    {
        this.gateNameBlockHolder = gateNameBlockHolder;
    }

    /**
     * Sets the gate network.
     * 
     * @param gateNetwork
     *            the new gate network
     */
    public void setGateNetwork(final StargateNetwork gateNetwork)
    {
        this.gateNetwork = gateNetwork;
    }

    /**
     * Sets the gate owner.
     * 
     * @param gateOwner
     *            the new gate owner
     */
    public void setGateOwner(final String gateOwner)
    {
        this.gateOwner = gateOwner;
    }

    /**
     * Sets the gate teleport location.
     * 
     * @param gatePlayerTeleportLocation
     *            the new gate player teleport location
     */
    public void setGatePlayerTeleportLocation(final Location gatePlayerTeleportLocation)
    {
        this.gatePlayerTeleportLocation = gatePlayerTeleportLocation;
    }

    /**
     * Sets the gate recently active.
     * 
     * @param gateRecentlyActive
     *            the new gate recently active
     */
    private void setGateRecentlyActive(final boolean gateRecentlyActive)
    {
        this.gateRecentlyActive = gateRecentlyActive;
    }

    /**
     * Sets the gate redstone activation block.
     * 
     * @param gateRedstoneDialActivationBlock
     *            the new gate redstone dial activation block
     */
    public void setGateRedstoneDialActivationBlock(final Block gateRedstoneDialActivationBlock)
    {
        this.gateRedstoneDialActivationBlock = gateRedstoneDialActivationBlock;
    }

    /**
     * Sets the gate redstone gate activated block.
     * 
     * @param gateRedstoneGateActivatedBlock
     *            the new gate redstone gate activated block
     */
    public void setGateRedstoneGateActivatedBlock(final Block gateRedstoneGateActivatedBlock)
    {
        this.gateRedstoneGateActivatedBlock = gateRedstoneGateActivatedBlock;
    }

    /**
     * Sets the gate redstone powered.
     * 
     * @param gateRedstonePowered
     *            the new gate redstone powered
     */
    public void setGateRedstonePowered(final boolean gateRedstonePowered)
    {
        this.gateRedstonePowered = gateRedstonePowered;
    }

    /**
     * Sets the gate redstone dial change block.
     * 
     * @param gateRedstoneSignActivationBlock
     *            the new gate redstone sign activation block
     */
    public void setGateRedstoneSignActivationBlock(final Block gateRedstoneSignActivationBlock)
    {
        this.gateRedstoneSignActivationBlock = gateRedstoneSignActivationBlock;
    }

    /**
     * Sets the gate shape.
     * 
     * @param gateShape
     *            the new gate shape
     */
    public void setGateShape(final StargateShape gateShape)
    {
        this.gateShape = gateShape;
    }

    /**
     * Sets the gate shutdown task id.
     * 
     * @param gateShutdownTaskId
     *            the new gate shutdown task id
     */
    private void setGateShutdownTaskId(final int gateShutdownTaskId)
    {
        this.gateShutdownTaskId = gateShutdownTaskId;
    }

    /**
     * Sets the gate sign powered.
     * 
     * @param gateSignPowered
     *            the new gate sign powered
     */
    public void setGateSignPowered(final boolean gateSignPowered)
    {
        this.gateSignPowered = gateSignPowered;
    }

    /**
     * Sets the gate target.
     * 
     * @param gateTarget
     *            the new gate target
     */
    private void setGateTarget(final Stargate gateTarget)
    {
        this.gateTarget = gateTarget;
    }

    /**
     * Sets the gate temp sign target.
     * 
     * @param gateTempSignTarget
     *            the new gate temp sign target
     */
    public void setGateTempSignTarget(final long gateTempSignTarget)
    {
        this.gateTempSignTarget = gateTempSignTarget;
    }

    /**
     * Sets the gate temp target id.
     * 
     * @param gateTempTargetId
     *            the new gate temp target id
     */
    public void setGateTempTargetId(final long gateTempTargetId)
    {
        this.gateTempTargetId = gateTempTargetId;
    }

    /**
     * Sets the gate world.
     * 
     * @param gateWorld
     *            the new gate world
     */
    public void setGateWorld(final World gateWorld)
    {
        this.gateWorld = gateWorld;
    }

    /**
     * Sets the iris deactivation code.
     * 
     * @param idc
     *            the idc
     */
    public void setIrisDeactivationCode(final String idc)
    {
        // If empty string make sure to make lever area air instead of lever.
        if ((idc != null) && !idc.equals(""))
        {
            setGateIrisDeactivationCode(idc);
            setupIrisLever(true);
        }
        else
        {
            setIrisState(false);
            setupIrisLever(false);
            setGateIrisDeactivationCode("");
        }
    }

    /**
     * This method sets the iris state and toggles the iris lever.
     * Smart enough to know if the gate is active and set the proper
     * material in its interior.
     * 
     * @param irisactive
     *            true for iris on, false for off.
     */
    private void setIrisState(final boolean irisactive)
    {
        setGateIrisActive(irisactive);
        Material interior = Material.AIR;
        if (isGateIrisActive()) {
            interior = isGateCustom()
                ? getGateCustomIrisMaterial()
                : (getGateShape() != null ? getGateShape().getShapeIrisMaterial() : Material.STONE);
        } else if (isGateActive()) {
            interior = isGateCustom()
                ? getGateCustomPortalMaterial()
                : (getGateShape() != null ? getGateShape().getShapePortalMaterial() : Material.WATER);
        } else {
            interior = Material.AIR;
        }
        fillGateInterior(interior);
        if (getGateIrisLeverBlock() != null && getGateIrisLeverBlock().getType() == Material.LEVER) {
            setLeverPowered(getGateIrisLeverBlock(), isGateIrisActive());
        }
    }

    /**
     * Sets the loaded version.
     * 
     * @param loadedVersion
     *            the new loaded version
     */
    public void setLoadedVersion(final byte loadedVersion)
    {
        this.loadedVersion = loadedVersion;
    }

    /**
     * Setup or remove gate name sign.
     * 
     * @param create
     *            true to create, false to destroy
     */
    public void setupGateSign(final boolean create)
    {
        if (getGateNameBlockHolder() != null)
        {
            if (create)
            {
                final Block nameSign = getGateNameBlockHolder().getRelative(getGateFacing());
                getGateStructureBlocks().add(nameSign.getLocation());
                placeWallSign(nameSign, getGateFacing());
                final Sign sign = (Sign) nameSign.getState();
                sign.setLine(0, "-" + getGateName() + "-");

                if (getGateNetwork() != null)
                {
                    sign.setLine(1, "N:" + getGateNetwork().getNetworkName());
                }

                if (getGateOwner() != null)
                {
                    sign.setLine(2, "O:" + getGateOwner());
                }
                sign.update(true);

            }
            else
            {
                final Block nameSign = getGateNameBlockHolder().getRelative(getGateFacing());
                if (nameSign.getType() == Material.OAK_WALL_SIGN)
                {
                    getGateStructureBlocks().remove(nameSign.getLocation());
                    nameSign.setType(Material.AIR);
                }
            }
        }
    }

    /**
     * Setup or remove IRIS control lever.
     * 
     * @param create
     *            true for create, false for destroy.
     */
    public void setupIrisLever(final boolean create)
    {
        if ((getGateIrisLeverBlock() == null) && (getGateShape() != null) && !(getGateShape() instanceof Stargate3DShape))
        {
            setGateIrisLeverBlock(getGateDialLeverBlock().getRelative(BlockFace.DOWN));
        }
        if (getGateIrisLeverBlock() != null)
        {
            if (create)
            {
                getGateStructureBlocks().add(getGateIrisLeverBlock().getLocation());
                placeLever(getGateIrisLeverBlock(), getGateFacing(), false);
            }
            else
            {
                if (getGateIrisLeverBlock().getType() == Material.LEVER)
                {
                    getGateStructureBlocks().remove(getGateIrisLeverBlock().getLocation());
                    getGateIrisLeverBlock().setType(Material.AIR);
                }
            }
        }
    }

    /**
     * Sets the up redstone connections (create or delete).
     * 
     * @param create
     *            true to create redstone connections, false to delete.
     */
    public void setupRedstone(final boolean create)
    {
        if (isGateSignPowered())
        {
            setupRedstoneDialWire(create);
            setupRedstoneSignDialWire(create);
        }
        setupRedstoneGateActivatedLever(create);
    }

    /**
     * Sets the up redstone dial wire.
     * 
     * @param create
     *            the new redstone dial wire
     */
    private void setupRedstoneDialWire(final boolean create)
    {
        if (getGateRedstoneDialActivationBlock() != null)
        {
            if (create)
            {
                getGateStructureBlocks().add(getGateRedstoneDialActivationBlock().getLocation());
                getGateRedstoneDialActivationBlock().setType(Material.REDSTONE_WIRE);
            }
            else
            {
                if (getGateRedstoneGateActivatedBlock().getType() == Material.REDSTONE_WIRE)
                {
                    getGateStructureBlocks().remove(getGateRedstoneDialActivationBlock().getLocation());
                    getGateRedstoneDialActivationBlock().setType(Material.AIR);
                }
            }
        }
    }

    /**
     * Sets the up redstone gate activated Lever.
     * 
     * @param create
     *            the new redstone gate activated lever
     */
    private void setupRedstoneGateActivatedLever(final boolean create)
    {
        if (getGateRedstoneGateActivatedBlock() != null)
        {
            if (create)
            {
                getGateStructureBlocks().add(getGateRedstoneGateActivatedBlock().getLocation());
                placeLever(getGateRedstoneGateActivatedBlock(), BlockFace.SOUTH, false);
            }
            else
            {
                if (getGateRedstoneGateActivatedBlock().getType() == Material.LEVER)
                {
                    getGateStructureBlocks().remove(getGateRedstoneGateActivatedBlock().getLocation());
                    getGateRedstoneGateActivatedBlock().setType(Material.AIR);
                }
            }
        }
    }

    /**
     * Sets the up redstone sign dial wire.
     * 
     * @param create
     *            the new redstone sign dial wire
     */
    private void setupRedstoneSignDialWire(final boolean create)
    {
        if (getGateRedstoneSignActivationBlock() != null)
        {
            if (create)
            {
                getGateStructureBlocks().add(getGateRedstoneSignActivationBlock().getLocation());
                getGateRedstoneSignActivationBlock().setType(Material.REDSTONE_WIRE);
            }
            else
            {
                if (getGateRedstoneGateActivatedBlock().getType() == Material.REDSTONE_WIRE)
                {
                    getGateStructureBlocks().remove(getGateRedstoneSignActivationBlock().getLocation());
                    getGateRedstoneSignActivationBlock().setType(Material.AIR);
                }
            }
        }
    }

    /**
     * Shutdown stargate.
     * 
     * @param timer
     *            true if we want to spawn after shutdown timer.
     */
    public void shutdownStargate(final boolean timer)
    {
        if (getGateShutdownTaskId() > 0)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ShutdownTaskID \"" + getGateShutdownTaskId() + "\" cancelled.");
            WormholeXTreme.getScheduler().cancelTask(getGateShutdownTaskId());
            setGateShutdownTaskId( -1);
        }

        if (getGateTarget() != null)
        {
            getGateTarget().shutdownStargate(true);
        }

        setGateTarget(null);
        if (timer)
        {
            setGateRecentlyActive(true);
        }
        setGateActive(false);

        lightStargate(false);
        toggleDialLeverState(false);
        toggleRedstoneGateActivatedPower();
        // Only set back to air if iris isn't on.
        // If the iris should be on, we will make it that way.
        if (isGateIrisDefaultActive())
        {
            setIrisState(isGateIrisDefaultActive());
        }
        else if ( !isGateIrisActive())
        {
            fillGateInterior(Material.AIR);
        }

        if (timer)
        {
            startAfterShutdownTimer();
        }

        WorldUtils.scheduleChunkUnload(getGatePlayerTeleportLocation().getBlock());
    }

    /**
     * Start activation timer.
     * 
     * @param p
     *            the p
     */
    public void startActivationTimer(final Player p)
    {
        if (getGateActivateTaskId() > 0)
        {
            WormholeXTreme.getScheduler().cancelTask(getGateActivateTaskId());
        }

        final int timeout = ConfigManager.getTimeoutActivate() * 20;
        setGateActivateTaskId(WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, p, ActionToTake.DEACTIVATE), timeout));
        WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ActivateTaskID \"" + getGateActivateTaskId() + "\" created.");
    }

    /**
     * After shutdown of stargate, spawn off task to set RecentActive = false;
     * This way we can depend on RecentActive for gate fire/lava protection.
     */
    private void startAfterShutdownTimer()
    {
        if (getGateAfterShutdownTaskId() > 0)
        {
            WormholeXTreme.getScheduler().cancelTask(getGateAfterShutdownTaskId());
        }
        final int timeout = 60;
        setGateAfterShutdownTaskId(WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, ActionToTake.AFTERSHUTDOWN), timeout));
        WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" AfterShutdownTaskID \"" + getGateAfterShutdownTaskId() + "\" created.");
        if (getGateAfterShutdownTaskId() == -1)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Failed to schdule wormhole after shutdown, received task id of -1.");
            setGateRecentlyActive(false);
        }
    }

    /**
     * Stop activation timer.
     * 
     */
    public void stopActivationTimer()
    {
        if (getGateActivateTaskId() > 0)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ActivateTaskID \"" + getGateActivateTaskId() + "\" cancelled.");
            WormholeXTreme.getScheduler().cancelTask(getGateActivateTaskId());
            setGateActivateTaskId( -1);
        }
    }

    /**
     * After shutdown stargate.
     */
    public void stopAfterShutdownTimer()
    {
        if (getGateAfterShutdownTaskId() > 0)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" AfterShutdownTaskID \"" + getGateAfterShutdownTaskId() + "\" cancelled.");
            WormholeXTreme.getScheduler().cancelTask(getGateAfterShutdownTaskId());
            setGateAfterShutdownTaskId( -1);
        }
        setGateRecentlyActive(false);
    }

    /**
     * Teleport sign clicked.
     */
    public void teleportSignClicked()
    {
        synchronized (getGateNetwork().getNetworkGateLock())
        {
            // Ensure a wall sign exists and is facing the correct way
            placeWallSign(getGateDialSignBlock(), getGateFacing());
            setGateDialSign((Sign) getGateDialSignBlock().getState());
            getGateDialSign().setLine(0, "-" + getGateName() + "-");
            if (getGateDialSignIndex() == -1)
            {
                setGateDialSignIndex(getGateDialSignIndex() + 1);
            }
            if ((getGateNetwork().getNetworkSignGateList().size() == 0) || (getGateNetwork().getNetworkSignGateList().size() == 1))
            {
                getGateDialSign().setLine(1, "");
                getGateDialSign().setLine(2, "No Other Gates");
                getGateDialSign().setLine(3, "");
                getGateDialSign().update();
                setGateDialSignTarget(null);
                return;
            }

            if (getGateDialSignIndex() >= getGateNetwork().getNetworkSignGateList().size())
            {
                setGateDialSignIndex(0);
            }

            if (getGateNetwork().getNetworkSignGateList().get(getGateDialSignIndex()).getGateName().equals(getGateName()))
            {
                setGateDialSignIndex(getGateDialSignIndex() + 1);
                if (getGateDialSignIndex() == getGateNetwork().getNetworkSignGateList().size())
                {
                    setGateDialSignIndex(0);
                }
            }

            if (getGateNetwork().getNetworkSignGateList().size() == 2)
            {
                getGateSignOrder().clear();
                getGateSignOrder().put(Integer.valueOf(2), getGateNetwork().getNetworkSignGateList().get(getGateDialSignIndex()));

                getGateDialSign().setLine(1, "");
                getGateDialSign().setLine(2, ">" + getGateSignOrder().get(Integer.valueOf(2)).getGateName() + "<");
                getGateDialSign().setLine(3, "");
                setGateDialSignTarget(getGateNetwork().getNetworkSignGateList().get(getGateDialSignIndex()));
            }
            else if (getGateNetwork().getNetworkSignGateList().size() == 3)
            {
                getGateSignOrder().clear();
                int orderIndex = 1;
                //SignIndex++;
                while (getGateSignOrder().size() < 2)
                {
                    if (getGateDialSignIndex() >= getGateNetwork().getNetworkSignGateList().size())
                    {
                        setGateDialSignIndex(0);
                    }

                    if (getGateNetwork().getNetworkSignGateList().get(getGateDialSignIndex()).getGateName().equals(getGateName()))
                    {
                        setGateDialSignIndex(getGateDialSignIndex() + 1);
                        if (getGateDialSignIndex() == getGateNetwork().getNetworkSignGateList().size())
                        {
                            setGateDialSignIndex(0);
                        }
                    }

                    getGateSignOrder().put(Integer.valueOf(orderIndex), getGateNetwork().getNetworkSignGateList().get(getGateDialSignIndex()));
                    orderIndex++;
                    if (orderIndex == 4)
                    {
                        orderIndex = 1;
                    }
                    setGateDialSignIndex(getGateDialSignIndex() + 1);
                }

                getGateDialSign().setLine(1, getGateSignOrder().get(Integer.valueOf(1)).getGateName());
                getGateDialSign().setLine(2, ">" + getGateSignOrder().get(Integer.valueOf(2)).getGateName() + "<");
                getGateDialSign().setLine(3, "");

                setGateDialSignTarget(getGateSignOrder().get(Integer.valueOf(2)));
                setGateDialSignIndex(getGateNetwork().getNetworkSignGateList().indexOf(getGateSignOrder().get(Integer.valueOf(2))));
            }
            else
            {
                getGateSignOrder().clear();
                int orderIndex = 1;
                while (getGateSignOrder().size() < 3)
                {
                    if (getGateDialSignIndex() == getGateNetwork().getNetworkSignGateList().size())
                    {
                        setGateDialSignIndex(0);
                    }

                    if (getGateNetwork().getNetworkSignGateList().get(getGateDialSignIndex()).getGateName().equals(getGateName()))
                    {
                        setGateDialSignIndex(getGateDialSignIndex() + 1);
                        if (getGateDialSignIndex() == getGateNetwork().getNetworkSignGateList().size())
                        {
                            setGateDialSignIndex(0);
                        }
                    }

                    getGateSignOrder().put(Integer.valueOf(orderIndex), getGateNetwork().getNetworkSignGateList().get(getGateDialSignIndex()));
                    orderIndex++;

                    setGateDialSignIndex(getGateDialSignIndex() + 1);
                }

                getGateDialSign().setLine(1, getGateSignOrder().get(Integer.valueOf(3)).getGateName());
                getGateDialSign().setLine(2, ">" + getGateSignOrder().get(Integer.valueOf(2)).getGateName() + "<");
                getGateDialSign().setLine(3, getGateSignOrder().get(Integer.valueOf(1)).getGateName());

                setGateDialSignTarget(getGateSignOrder().get(Integer.valueOf(2)));
                setGateDialSignIndex(getGateNetwork().getNetworkSignGateList().indexOf(getGateSignOrder().get(Integer.valueOf(2))));
            }
            getGateDialSign().update(true);
        }

        // getGateTeleportSign().setData(getGateTeleportSign().getData());

    }

    /**
     * Timeout stargate.
     * 
     * @param p
     *            the p
     */
    public void timeoutStargate(final Player p)
    {
        if (getGateActivateTaskId() > 0)
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ActivateTaskID \"" + getGateActivateTaskId() + "\" timed out.");
            setGateActivateTaskId( -1);
        }
        // Deactivate if player still hasn't picked a target.
        Stargate s = null;
        if (p != null)
        {
            s = StargateManager.removeActivatedStargate(p);
        }
        else
        {
            s = this;
        }

        // Only send a message if the gate was still in the remotely activated gates list.
        if (s != null)
        {
            // Make sure to reset iris if it should be on.
            if (isGateIrisDefaultActive())
            {
                setIrisState(isGateIrisDefaultActive());
            }
            if (isGateLightsActive())
            {
                s.lightStargate(false);
            }

            if (p != null)
            {
                p.sendMessage("Gate: " + getGateName() + " timed out and deactivated.");
            }
        }
    }

    /**
     * Set the dial button and lever block state based on gate activation status.
     * 
     * @param regenerate
     *            true, to replace missing activation lever.
     */
    public void toggleDialLeverState(final boolean regenerate)
    {
        if (getGateDialLeverBlock() != null)
        {
            if (isGateActive())
            {
                WorldUtils.scheduleChunkLoad(getGateDialLeverBlock());
            }
            Material leverType = getGateDialLeverBlock().getType();
            if (regenerate)
            {
                placeLever(getGateDialLeverBlock(), getGateFacing(), isGateActive());
                leverType = getGateDialLeverBlock().getType();
            }
            switch (leverType)
            {
                case STONE_BUTTON :
                    getGateDialLeverBlock().setType(Material.LEVER);
                    WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Automaticially replaced Button on gate \"" + getGateName() + "\" with Lever.");
                    setLeverPowered(getGateDialLeverBlock(), isGateActive());
                    break;
                case LEVER :
                    setLeverPowered(getGateDialLeverBlock(), isGateActive());
                    break;
                default :
                    break;
            }
            if ( !isGateActive())
            {
                WorldUtils.scheduleChunkUnload(getGateDialLeverBlock());
            }
            WormholeXTreme.getThisPlugin().prettyLog(Level.FINE, false, "Dial Button Lever Gate: \"" + getGateName() + "\" Material: \"" + leverType + "\"");
        }
    }

    /**
     * Toggle the iris state.
     * 
     * @param setDefault
     *            true to set the toggled state as the default state.
     */
    public void toggleIrisActive(final boolean setDefault)
    {
        setGateIrisActive( !isGateIrisActive());
        setIrisState(isGateIrisActive());
        if (setDefault)
        {
            setGateIrisDefaultActive(isGateIrisActive());
        }
    }

    /**
     * Toggle redstone gate activated power.
     */
    private void toggleRedstoneGateActivatedPower()
    {
        if (isGateRedstonePowered() && (getGateRedstoneGateActivatedBlock() != null) && (getGateRedstoneGateActivatedBlock().getType() == Material.LEVER))
        {
            setLeverPowered(getGateRedstoneGateActivatedBlock(), isGateActive());
        }
    }

    /**
     * Try click teleport sign. This is the same as {@link Stargate#tryClickTeleportSign(Block, Player)} with Player set
     * to null.
     * 
     * @param clicked
     *            the clicked
     * @return true, if successful
     */
    public boolean tryClickTeleportSign(final Block clicked)
    {
        return tryClickTeleportSign(clicked, null);
    }

    /**
     * Try click teleport sign.
     * 
     * @param clicked
     *            the clicked
     * @param player
     *            the player
     * @return true, if successful
     */
    public boolean tryClickTeleportSign(final Block clicked, final Player player)
    {
        if ((getGateDialSign() == null) && (getGateDialSignBlock() != null))
        {
            if (getGateDialSignBlock().getType() == Material.OAK_WALL_SIGN)
            {
                setGateDialSignIndex( -1);
                getGateDialSignBlock().setType(Material.AIR);
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, player, ActionToTake.DIAL_SIGN_CLICK));
            }
        }
        else if (WorldUtils.isSameBlock(clicked, getGateDialSignBlock()))
        {
            getGateDialSignBlock().setType(Material.AIR);
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, player, ActionToTake.DIAL_SIGN_CLICK));
            return true;
        }

        return false;
    }

    /**
     * Serializes this Stargate into a byte array for database storage.
     * 
     * @return byte array containing the serialized Stargate data
     */
    public byte[] toByteArray() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            
            Map<String, Object> data = new HashMap<>();
            
            // Primitive fields
            data.put("loadedVersion", loadedVersion);
            data.put("gateId", gateId);
            data.put("gateName", gateName);
            data.put("gateOwner", gateOwner);
            data.put("gateNetwork", gateNetwork != null ? gateNetwork.getNetworkName() : null);
            data.put("gateShape", gateShape != null ? gateShape.getShapeName() : null);
            data.put("gateWorld", gateWorld != null ? gateWorld.getUID().toString() : null);
            data.put("gateActive", gateActive);
            data.put("gateRecentlyActive", gateRecentlyActive);
            data.put("gateFacing", gateFacing != null ? gateFacing.name() : null);
                       data.put("gateLightsActive", gateLightsActive);
            data.put("gateSignPowered", gateSignPowered);
            data.put("gateRedstonePowered", gateRedstonePowered);
            data.put("gateTempSignTarget", gateTempSignTarget);
            data.put("gateDialSignIndex", gateDialSignIndex);
            data.put("gateTempTargetId", gateTempTargetId);
            data.put("gateIrisDeactivationCode", gateIrisDeactivationCode);
            data.put("gateIrisActive", gateIrisActive);
            data.put("gateIrisDefaultActive", gateIrisDefaultActive);
            data.put("gateCustom", gateCustom);
            data.put("gateCustomStructureMaterial", gateCustomStructureMaterial != null ? gateCustomStructureMaterial.name() : null);
            data.put("gateCustomPortalMaterial", gateCustomPortalMaterial != null ? gateCustomPortalMaterial.name() : null);
            data.put("gateCustomLightMaterial", gateCustomLightMaterial != null ? gateCustomLightMaterial.name() : null);
            data.put("gateCustomIrisMaterial", gateCustomIrisMaterial != null ? gateCustomIrisMaterial.name() : null);
            data.put("gateCustomWooshTicks", gateCustomWooshTicks);
            data.put("gateCustomLightTicks", gateCustomLightTicks);
            data.put("gateCustomWooshDepth", gateCustomWooshDepth);
            data.put("gateCustomWooshDepthSquared", gateCustomWooshDepthSquared);
            
            // Location data
            data.put("gatePlayerTeleportLocation", serializeLocation(gatePlayerTeleportLocation));
            data.put("gateMinecartTeleportLocation", serializeLocation(gateMinecartTeleportLocation));
            data.put("gateDialLeverBlock", serializeLocation(gateDialLeverBlock != null ? gateDialLeverBlock.getLocation() : null));
            data.put("gateIrisLeverBlock", serializeLocation(gateIrisLeverBlock != null ? gateIrisLeverBlock.getLocation() : null));
            data.put("gateDialSignBlock", serializeLocation(gateDialSignBlock != null ? gateDialSignBlock.getLocation() : null));
            data.put("gateRedstoneDialActivationBlock", serializeLocation(gateRedstoneDialActivationBlock != null ? gateRedstoneDialActivationBlock.getLocation() : null));
            data.put("gateRedstoneSignActivationBlock", serializeLocation(gateRedstoneSignActivationBlock != null ? gateRedstoneSignActivationBlock.getLocation() : null));
            data.put("gateRedstoneGateActivatedBlock", serializeLocation(gateRedstoneGateActivatedBlock != null ? gateRedstoneGateActivatedBlock.getLocation() : null));
            data.put("gateNameBlockHolder", serializeLocation(gateNameBlockHolder != null ? gateNameBlockHolder.getLocation() : null));
            
            // Collections
            data.put("gateStructureBlocks", serializeLocationList(gateStructureBlocks));
            data.put("gatePortalBlocks", serializeLocationList(gatePortalBlocks));
            
            // Serialize light blocks (List of Lists of Locations)
            List<List<Map<String, Object>>> serializedLightBlocks = new ArrayList<>();
            for (List<Location> blockList : gateLightBlocks) {
                serializedLightBlocks.add(serializeLocationList(blockList));
            }
            data.put("gateLightBlocks", serializedLightBlocks);
            
            // Serialize woosh blocks (List of Lists of Locations)
            List<List<Map<String, Object>>> serializedWooshBlocks = new ArrayList<>();
            for (List<Location> blockList : gateWooshBlocks) {
                serializedWooshBlocks.add(serializeLocationList(blockList));
            }
            data.put("gateWooshBlocks", serializedWooshBlocks);
            
            // Write the data map to the output stream
            oos.writeObject(data);
            oos.flush();
            gzos.finish();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Failed to serialize Stargate " + gateName + ": " + e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * Deserializes a Stargate from a byte array.
     * 
     * @param data the serialized Stargate data
     * @return the deserialized Stargate, or null if deserialization failed
     */
    @SuppressWarnings("unchecked")
    public static Stargate fromByteArray(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ObjectInputStream ois = new ObjectInputStream(gzis)) {
            
            Map<String, Object> map = (Map<String, Object>) ois.readObject();
            Stargate stargate = new Stargate();
            
            // Primitive fields
            stargate.loadedVersion = map.containsKey("loadedVersion") ? ((Number) map.get("loadedVersion")).byteValue() : -1;
            stargate.gateId = map.containsKey("gateId") ? ((Number) map.get("gateId")).intValue() : -1;
            stargate.gateName = (String) map.getOrDefault("gateName", "");
            stargate.gateOwner = (String) map.get("gateOwner");
            
            String networkName = (String) map.get("gateNetwork");
            if (networkName != null) {
                stargate.gateNetwork = StargateManager.getStargateNetworks().get(networkName.toLowerCase());
            }
            
            String shapeName = (String) map.get("gateShape");
            if (shapeName != null) {
                // Shapes are loaded from files/resources; fallback to existing shape if names match
                if (stargate.getGateShape() != null && stargate.getGateShape().getShapeName().equalsIgnoreCase(shapeName)) {
                    // keep existing
                }
            }
            
            String worldUuidStr = (String) map.get("gateWorld");
            if (worldUuidStr != null) {
                try {
                    UUID worldUuid = UUID.fromString(worldUuidStr);
                    stargate.gateWorld = Bukkit.getWorld(worldUuid);
                } catch (IllegalArgumentException e) {
                    WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "Invalid world UUID in deserialized Stargate: " + worldUuidStr);
                }
            }
            
            stargate.gateActive = (boolean) map.getOrDefault("gateActive", false);
            stargate.gateRecentlyActive = (boolean) map.getOrDefault("gateRecentlyActive", false);
            
            String facingStr = (String) map.get("gateFacing");
            if (facingStr != null) {
                stargate.gateFacing = BlockFace.valueOf(facingStr);
            }
            
            stargate.gateLightsActive = (boolean) map.getOrDefault("gateLightsActive", false);
            stargate.gateSignPowered = (boolean) map.getOrDefault("gateSignPowered", false);
            stargate.gateRedstonePowered = (boolean) map.getOrDefault("gateRedstonePowered", false);
            stargate.gateTempSignTarget = map.containsKey("gateTempSignTarget") ? ((Number) map.get("gateTempSignTarget")).longValue() : -1;
            stargate.gateDialSignIndex = map.containsKey("gateDialSignIndex") ? ((Number) map.get("gateDialSignIndex")).intValue() : 0;
            stargate.gateTempTargetId = map.containsKey("gateTempTargetId") ? ((Number) map.get("gateTempTargetId")).longValue() : -1;
            stargate.gateIrisDeactivationCode = (String) map.getOrDefault("gateIrisDeactivationCode", "");
            stargate.gateIrisActive = (boolean) map.getOrDefault("gateIrisActive", false);
            stargate.gateIrisDefaultActive = (boolean) map.getOrDefault("gateIrisDefaultActive", false);
            stargate.gateCustom = (boolean) map.getOrDefault("gateCustom", false);
            
            // Material deserialization
            String structureMat = (String) map.get("gateCustomStructureMaterial");
            if (structureMat != null) {
                try {
                    stargate.gateCustomStructureMaterial = Material.valueOf(structureMat);
                } catch (IllegalArgumentException e) {
                    WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "Invalid structure material: " + structureMat);
                }
            }
            
            String portalMat = (String) map.get("gateCustomPortalMaterial");
            if (portalMat != null) {
                try {
                    stargate.gateCustomPortalMaterial = Material.valueOf(portalMat);
                } catch (IllegalArgumentException e) {
                    WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "Invalid portal material: " + portalMat);
                }
            }
            
            String lightMat = (String) map.get("gateCustomLightMaterial");
            if (lightMat != null) {
                try {
                    stargate.gateCustomLightMaterial = Material.valueOf(lightMat);
                } catch (IllegalArgumentException e) {
                    WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "Invalid light material: " + lightMat);
                }
            }
            
            String irisMat = (String) map.get("gateCustomIrisMaterial");
            if (irisMat != null) {
                try {
                    stargate.gateCustomIrisMaterial = Material.valueOf(irisMat);
                } catch (IllegalArgumentException e) {
                    WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "Invalid iris material: " + irisMat);
                }
            }
            
            stargate.gateCustomWooshTicks = map.containsKey("gateCustomWooshTicks") ? ((Number) map.get("gateCustomWooshTicks")).intValue() : -1;
            stargate.gateCustomLightTicks = map.containsKey("gateCustomLightTicks") ? ((Number) map.get("gateCustomLightTicks")).intValue() : -1;
            stargate.gateCustomWooshDepth = map.containsKey("gateCustomWooshDepth") ? ((Number) map.get("gateCustomWooshDepth")).intValue() : -1;
            stargate.gateCustomWooshDepthSquared = map.containsKey("gateCustomWooshDepthSquared") ? ((Number) map.get("gateCustomWooshDepthSquared")).intValue() : -1;
            
            // Deserialize locations
            stargate.gatePlayerTeleportLocation = deserializeLocation((Map<String, Object>) map.get("gatePlayerTeleportLocation"));
            stargate.gateMinecartTeleportLocation = deserializeLocation((Map<String, Object>) map.get("gateMinecartTeleportLocation"));
            
            // Deserialize block locations and get blocks
            Location dialLeverLoc = deserializeLocation((Map<String, Object>) map.get("gateDialLeverBlock"));
            stargate.gateDialLeverBlock = dialLeverLoc != null ? dialLeverLoc.getBlock() : null;
            
            Location irisLeverLoc = deserializeLocation((Map<String, Object>) map.get("gateIrisLeverBlock"));
            stargate.gateIrisLeverBlock = irisLeverLoc != null ? irisLeverLoc.getBlock() : null;
            
            Location dialSignLoc = deserializeLocation((Map<String, Object>) map.get("gateDialSignBlock"));
            stargate.gateDialSignBlock = dialSignLoc != null ? dialSignLoc.getBlock() : null;
            
            Location redstoneDialLoc = deserializeLocation((Map<String, Object>) map.get("gateRedstoneDialActivationBlock"));
            stargate.gateRedstoneDialActivationBlock = redstoneDialLoc != null ? redstoneDialLoc.getBlock() : null;
            
            Location redstoneSignLoc = deserializeLocation((Map<String, Object>) map.get("gateRedstoneSignActivationBlock"));
            stargate.gateRedstoneSignActivationBlock = redstoneSignLoc != null ? redstoneSignLoc.getBlock() : null;
            
            Location redstoneGateLoc = deserializeLocation((Map<String, Object>) map.get("gateRedstoneGateActivatedBlock"));
            stargate.gateRedstoneGateActivatedBlock = redstoneGateLoc != null ? redstoneGateLoc.getBlock() : null;
            
            Location nameBlockLoc = deserializeLocation((Map<String, Object>) map.get("gateNameBlockHolder"));
            stargate.gateNameBlockHolder = nameBlockLoc != null ? nameBlockLoc.getBlock() : null;
            
            // Deserialize collections
            stargate.gateStructureBlocks.clear();
            stargate.gateStructureBlocks.addAll(deserializeLocationList((List<Map<String, Object>>) map.get("gateStructureBlocks")));
            
            stargate.gatePortalBlocks.clear();
            stargate.gatePortalBlocks.addAll(deserializeLocationList((List<Map<String, Object>>) map.get("gatePortalBlocks")));
            
            // Deserialize light blocks (List of Lists of Locations)
            stargate.gateLightBlocks.clear();
            List<List<Map<String, Object>>> serializedLightBlocks = (List<List<Map<String, Object>>>) map.get("gateLightBlocks");
            if (serializedLightBlocks != null) {
                for (List<Map<String, Object>> blockList : serializedLightBlocks) {
                    stargate.gateLightBlocks.add(new ArrayList<>(deserializeLocationList(blockList)));
                }
            }
            
            // Deserialize woosh blocks (List of Lists of Locations)
            stargate.gateWooshBlocks.clear();
            List<List<Map<String, Object>>> serializedWooshBlocks = (List<List<Map<String, Object>>>) map.get("gateWooshBlocks");
            if (serializedWooshBlocks != null) {
                for (List<Map<String, Object>> blockList : serializedWooshBlocks) {
                    stargate.gateWooshBlocks.add(new ArrayList<>(deserializeLocationList(blockList)));
                }
            }
            
            // After deserialization, we need to re-initialize some fields
            if (stargate.gateDialSignBlock != null && stargate.gateDialSignBlock.getState() instanceof Sign) {
                stargate.gateDialSign = (Sign) stargate.gateDialSignBlock.getState();
            }
            
            return stargate;
            
        } catch (Exception e) {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Failed to deserialize Stargate: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper method to serialize a Location to a Map.
     * 
     * @param loc the Location to serialize
     * @return a Map containing the serialized Location data, or null if loc is null
     */
    private static Map<String, Object> serializeLocation(Location loc) {
        if (loc == null) {
            return null;
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("world", loc.getWorld() != null ? loc.getWorld().getUID().toString() : null);
        map.put("x", loc.getX());
        map.put("y", loc.getY());
        map.put("z", loc.getZ());
        map.put("yaw", loc.getYaw());
        map.put("pitch", loc.getPitch());
        
        return map;
    }
    
    /**
     * Helper method to deserialize a Location from a Map.
     * 
     * @param map the Map containing the serialized Location data
     * @return the deserialized Location, or null if map is null or invalid
     */
    private static Location deserializeLocation(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        
        try {
            String worldUuidStr = (String) map.get("world");
            if (worldUuidStr == null) {
                return null;
            }
            
            UUID worldUuid = UUID.fromString(worldUuidStr);
            World world = Bukkit.getWorld(worldUuid);
            if (world == null) {
                return null;
            }
            
            double x = ((Number) map.getOrDefault("x", 0.0)).doubleValue();
            double y = ((Number) map.getOrDefault("y", 0.0)).doubleValue();
            double z = ((Number) map.getOrDefault("z", 0.0)).doubleValue();
            float yaw = ((Number) map.getOrDefault("yaw", 0.0f)).floatValue();
            float pitch = ((Number) map.getOrDefault("pitch", 0.0f)).floatValue();
            
            return new Location(world, x, y, z, yaw, pitch);
            
        } catch (Exception e) {
            WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "Failed to deserialize location: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper method to serialize a List of Locations to a List of Maps.
     * 
     * @param locations the List of Locations to serialize
     * @return a List of Maps containing the serialized Location data
     */
    private static List<Map<String, Object>> serializeLocationList(List<Location> locations) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (locations != null) {
            for (Location loc : locations) {
                Map<String, Object> serialized = serializeLocation(loc);
                if (serialized != null) {
                    result.add(serialized);
                }
            }
        }
        return result;
    }
    
    /**
     * Helper method to deserialize a List of Maps to a List of Locations.
     * 
     * @param locationMaps the List of Maps containing the serialized Location data
     * @return a List of deserialized Locations
     */
    private static List<Location> deserializeLocationList(List<Map<String, Object>> locationMaps) {
        List<Location> result = new ArrayList<>();
        if (locationMaps != null) {
            for (Map<String, Object> map : locationMaps) {
                Location loc = deserializeLocation(map);
                if (loc != null) {
                    result.add(loc);
                }
            }
        }
        return result;
    }
    
    /**
     * Updates this Stargate's fields from another Stargate instance.
     * This is typically used to update a cached Stargate with fresh data from the database.
     * 
     * @param updatedGate the Stargate with updated data
     */
    public void updateFrom(Stargate updatedGate) {
        if (updatedGate == null) {
            return;
        }
        
        // Update all fields that should be persisted
        this.gateName = updatedGate.gateName;
        this.gateOwner = updatedGate.gateOwner;
        this.gateNetwork = updatedGate.gateNetwork;
        this.gateShape = updatedGate.gateShape;
        this.gateWorld = updatedGate.gateWorld;
        this.gateFacing = updatedGate.gateFacing;
        this.gateIrisDeactivationCode = updatedGate.gateIrisDeactivationCode;
        this.gateIrisDefaultActive = updatedGate.gateIrisDefaultActive;
        this.gateCustom = updatedGate.gateCustom;
        this.gateCustomStructureMaterial = updatedGate.gateCustomStructureMaterial;
        this.gateCustomPortalMaterial = updatedGate.gateCustomPortalMaterial;
        this.gateCustomLightMaterial = updatedGate.gateCustomLightMaterial;
        this.gateCustomIrisMaterial = updatedGate.gateCustomIrisMaterial;
        this.gateCustomWooshTicks = updatedGate.gateCustomWooshTicks;
        this.gateCustomLightTicks = updatedGate.gateCustomLightTicks;
        this.gateCustomWooshDepth = updatedGate.gateCustomWooshDepth;
        this.gateCustomWooshDepthSquared = updatedGate.gateCustomWooshDepthSquared;
        
        // Update locations
        this.gatePlayerTeleportLocation = updatedGate.gatePlayerTeleportLocation != null ? 
                updatedGate.gatePlayerTeleportLocation.clone() : null;
        this.gateMinecartTeleportLocation = updatedGate.gateMinecartTeleportLocation != null ? 
                updatedGate.gateMinecartTeleportLocation.clone() : null;
        
        // Update block references
        this.gateDialLeverBlock = updatedGate.gateDialLeverBlock;
        this.gateIrisLeverBlock = updatedGate.gateIrisLeverBlock;
        this.gateDialSignBlock = updatedGate.gateDialSignBlock;
        this.gateRedstoneDialActivationBlock = updatedGate.gateRedstoneDialActivationBlock;
        this.gateRedstoneSignActivationBlock = updatedGate.gateRedstoneSignActivationBlock;
        this.gateRedstoneGateActivatedBlock = updatedGate.gateRedstoneGateActivatedBlock;
        this.gateNameBlockHolder = updatedGate.gateNameBlockHolder;
        
        // Update collections
        this.gateStructureBlocks.clear();
        this.gateStructureBlocks.addAll(updatedGate.gateStructureBlocks);
        
        this.gatePortalBlocks.clear();
        this.gatePortalBlocks.addAll(updatedGate.gatePortalBlocks);
        
        this.gateLightBlocks.clear();
        for (List<Location> blockList : updatedGate.gateLightBlocks) {
            this.gateLightBlocks.add(new ArrayList<>(blockList));
        }
        
        this.gateWooshBlocks.clear();
        for (List<Location> blockList : updatedGate.gateWooshBlocks) {
            this.gateWooshBlocks.add(new ArrayList<>(blockList));
        }
        
        // Update sign reference if needed
        if (this.gateDialSignBlock != null && this.gateDialSignBlock.getState() instanceof Sign) {
            this.gateDialSign = (Sign) this.gateDialSignBlock.getState();
        } else {
            this.gateDialSign = null;
        }
    }

    /**
     * Returns the blocks that make up this stargate.
     * @return List of blocks
     */
    public List<Block> getGateBlocks() {
        // TODO: Implement actual logic to return gate blocks
        return new ArrayList<>();
    }
}