/**
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
package com.wormhole_xtreme.wormhole.plugin;

import java.util.logging.Level;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.plugin.Plugin;

import com.wormhole_xtreme.wormhole.WormholeXTreme;
import com.wormhole_xtreme.wormhole.config.WormholeConfig;

/**
 * The Class WormholeWorldsSupport.
 * 
 * @author alron
 */
public class WormholeWorldsSupport
{

    /**
     * Check worlds version.
     * 
     * @param version
     *            the version
     * @return true, if successful
     */
    private static boolean checkWorldsVersion(final String version)
    {
        if ( !version.startsWith("0.5"))
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.SEVERE, false, "Not a supported version of Wormhole Worlds. Recommended is 0.5");
            return false;
        }
        return true;
    }

    /**
     * Disable wormhole worlds.
     */
    public static void disableWormholeWorlds()
    {
        if (WormholeXTreme.getWorldHandler() != null)
        {
            WormholeXTreme.setWorldHandler(null);
            WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, "Detached from Wormhole Worlds plugin.");
        }
    }

    /**
     * Enable wormhole worlds.
     */
    public static void enableWormholeWorlds()
    {
    if (WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.WORLDS_SUPPORT_ENABLED))
        {
            if (WormholeXTreme.getWorldHandler() == null)
            {
                final Plugin worldsTest = WormholeXTreme.getThisPlugin().getServer().getPluginManager().getPlugin("WormholeXTremeWorlds");
                if (worldsTest != null)
                {
                    final String version = worldsTest.getDescription().getVersion();
                    if (checkWorldsVersion(version))
                    {
                        try {
                            // Use reflection to avoid hard dependency
                            Object handler = worldsTest.getClass().getMethod("getWorldHandler").invoke(null);
                            WormholeXTreme.setWorldHandler((com.wormhole_xtreme.wormhole.WorldHandler) handler);
                            WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, "Attached to Wormhole Worlds version " + version);
                            // Worlds support means we can continue our load.
                            WormholeXTreme.registerEvents(false);
                            WormholeXTreme.runRegisterCommands();
                            WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, true, "Enable Completed.");
                        } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            WormholeXTreme.getThisPlugin().prettyLog(Level.WARNING, false, "Failed to attach to Wormhole Worlds: " + e.getMessage());
                        }
                    }
                }
                else
                {
                    WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, "Wormhole Worlds Plugin not yet available Stargates will not load until it enables.");
                }
            }
        }
        else
        {
            WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, "Wormhole X-Treme Worlds Plugin support disabled via settings.txt.");
        }
    }
}