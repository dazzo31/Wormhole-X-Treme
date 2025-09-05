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

import com.wormhole_xtreme.wormhole.WormholeXTreme;
import com.wormhole_xtreme.wormhole.config.WormholeConfig;

/**
 * The Class PermissionsSupport.
 * 
 * @author alron
 */
public class PermissionsSupport {

    /**
     * Check permissions version.
     * 
     * @param version
     *            the version
     */

    /**
     * Disable permissions.
     */
    public static void disablePermissions() { /* no-op */ }

    /**
     * Setup permissions.
     */
    public static void enablePermissions() {
        if (!WormholeXTreme.getThisPlugin().getWormholeConfig().get(WormholeConfig.PERMISSIONS_SUPPORT_DISABLE)) {
            WormholeXTreme.getThisPlugin().prettyLog(Level.INFO, false, "External Permissions integration is not available; using Vault if present.");
        }
    }
}
