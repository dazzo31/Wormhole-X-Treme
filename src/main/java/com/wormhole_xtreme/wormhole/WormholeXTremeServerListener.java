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

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;

import com.wormhole_xtreme.wormhole.config.WormholeConfig;
import com.wormhole_xtreme.wormhole.plugin.HelpSupport;
import com.wormhole_xtreme.wormhole.plugin.PermissionsSupport;
import com.wormhole_xtreme.wormhole.plugin.WormholeWorldsSupport;

/**
 * WormholeXTreme Server Listener.
 * 
 * @author Ben Echols (Lologarithm)
 * @author Dean Bailey (alron)
 */
class WormholeXTremeServerListener implements Listener
{

    /* (non-Javadoc)
     * @see org.bukkit.event.server.ServerListener#onPluginDisabled(org.bukkit.event.server.PluginEvent)
     */
    @EventHandler
    public void handlePluginDisable(final PluginDisableEvent event)
    {
        final WormholeConfig config = WormholeXTreme.getThisPlugin().getWormholeConfig();
        if (event.getPlugin().getDescription().getName().equals("Permissions") && !config.get(WormholeConfig.PERMISSIONS_SUPPORT_DISABLE))
        {
            PermissionsSupport.disablePermissions();
        }
        else if (event.getPlugin().getDescription().getName().equals("Help") && !config.get(WormholeConfig.HELP_SUPPORT_DISABLE))
        {
            HelpSupport.disableHelp();
        }
        else if (event.getPlugin().getDescription().getName().equals("WormholeXTremeWorlds") && config.get(WormholeConfig.WORLDS_SUPPORT_ENABLED))
        {
            WormholeWorldsSupport.disableWormholeWorlds();
        }
    }

    /* (non-Javadoc)
     * @see org.bukkit.event.server.ServerListener#onPluginEnabled(org.bukkit.event.server.PluginEvent)
     */
    @EventHandler
    public void handlePluginEnable(final PluginEnableEvent event)
    {
        final WormholeConfig config = WormholeXTreme.getThisPlugin().getWormholeConfig();
        if (event.getPlugin().getDescription().getName().equals("Permissions") && !config.get(WormholeConfig.PERMISSIONS_SUPPORT_DISABLE))
        {
            PermissionsSupport.enablePermissions();
        }
        else if (event.getPlugin().getDescription().getName().equals("Help") && !config.get(WormholeConfig.HELP_SUPPORT_DISABLE))
        {
            HelpSupport.enableHelp();
        }
        else if (event.getPlugin().getDescription().getName().equals("WormholeXTremeWorlds") && config.get(WormholeConfig.WORLDS_SUPPORT_ENABLED))
        {
            WormholeWorldsSupport.enableWormholeWorlds();
        }
    }
}
