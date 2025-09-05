# Wormhole X-Treme (Modernized)

Modern Spigot 1.20.4 rework of the classic Wormhole X-Treme plugin. Provides configurable Stargate-style wormholes: dialing, sign navigation, iris shields, activation timeouts, permissions integration, cooldowns, redstone hooks.

## ✨ Features
* 2D & 3D gate shapes (extensible via `GateShapes/` files)
* Command or Sign Powered navigation (cycle targets by right‑clicking the wall sign)
* Iris open/close security with optional IDC code
* Activation & shutdown timeouts (auto close), player cooldowns & build limits
* Vault permissions + fallback simple/built‑in mode (ConfigMe powered)
* Redstone: trigger dial, cycle sign, active state output
* Embedded HSQLDB + HikariCP (no external DB required)

## 🛠 2025 Modernization Highlights
* Replaced numeric block IDs/data with `Material` + `BlockData`
* Updated all sign/lever handling, centralized in `Stargate`
* Removed stationary water/lava legacy constants; modern portal material choices
* Added `WormholeConfig` (ConfigMe) replacing legacy `settings.txt`
* Vault integration and simplified permission modes
* World loading via `WorldCreator`; removed reliance on world numeric IDs
* Safer DB code (null checks, cleaned serialization helpers)
* Deprecated Sign API isolated (future: Adventure Components)

## 📦 Build
Requires JDK 21 & Maven.

```powershell
mvn clean package
```
Jar: `target/WormholeXTreme-<version>.jar`

## 🚀 Install
1. Copy jar to `plugins/`.
2. Start server (config + data folders populate).
3. Edit `plugins/WormholeXTreme/config.yml` as needed.
4. (Optional) Add/edit shape files in `GateShapes/`.
5. Reload or restart.

## 🔧 Key Config (`config.yml`)
* `timeouts.activate` – seconds gate stays lit awaiting dial
* `timeouts.shutdown` – auto shutdown after connection
* `cooldown.enabled` & `cooldown.group_*` – player use cooldowns
* `build_restriction.enabled` & groups – limit player gate counts
* `permissions.simple_mode` – toggle simple vs complex nodes
* `permissions.use_is_teleport` – require perm to teleport (not just activate)

## 🔐 Permissions
Simple mode:
`wormhole.simple.use`, `wormhole.simple.build`, `wormhole.simple.remove`, `wormhole.simple.config`

Complex examples:
`wormhole.use`, `wormhole.build`, `wormhole.admin`,
`wormhole.network.use.<net>`, `wormhole.network.build.<net>`,
`wormhole.build.groupone|grouptwo|grouptwo`,
`wormhole.cooldown.groupone|grouptwo|grouptwo`

## 🧪 Roadmap
* Adventure Component sign text
* Unit tests (shape parse, serialization, cooldown logic)
* Configurable portal / iris material enums
* Optional Paper events for performance metrics

## 📁 Structure
```
src/main/java        # Core plugin code
src/main/resources   # plugin.yml
GateShapes/          # Packaged default shapes
```
Runtime data lives under `plugins/WormholeXTreme/` after first launch.

## ⚖️ License
GPL-3.0 (see `LICENSE.txt`). Original authors retained.

## 🙏 Credits
Original: Lologarithm, alron  
Modernization: 2025 maintenance update

---
Issues & PRs welcome.
