# Wormhole X-Treme v1.0.4 (Modernized)

Travel instantly between locations using buildable **Stargates** with dial signs, activation levers, iris security, networks, and configurable materials.

## Key Modernization (1.0.x line)
- Java 21 + Spigot/Paper 1.20.4+ (tested on 1.21.x)
- Single shaded jar (no external lib folder)
- Embedded file-based HSQLDB (no external DB) stored in `plugins/WormholeXTreme/WormholeXTremeDB`
- Legacy `Settings.txt` auto-migrated to `config.yml`
- Legacy shape material names auto-mapped (STATIONARY_WATER, STATIONARY_LAVA, PORTAL, GLOWING_REDSTONE_ORE)
- INFO level startup summary lists loaded gate shapes

## Requirements
- Java 21 JVM
- Spigot or Paper 1.20.4+ (1.21+ recommended)
- (Optional) Vault for advanced permissions/economy integration

## Installation (Fresh)
1. Download latest `WormholeXTreme-<version>.jar` from releases.
2. Drop into `plugins/` directory.
3. Start server (default shapes + config created).
4. (Optional) Stop & edit `plugins/WormholeXTreme/config.yml`.
5. Restart – console should show: `Loaded N stargate shape(s): [...]`.

## Upgrade
1. Back up `plugins/WormholeXTreme/` (DB + shapes).
2. Replace old jar with new jar.
3. Start server (auto DB/config migration).
4. Verify shapes list and gate functionality.

## Data & Persistence
Embedded HSQLDB files (persistent across restarts):
- `WormholeXTremeDB.script` (schema + data)
- Runtime: `.lck`, `.log`, `.tmp`

Backup: stop server, copy `WormholeXTremeDB/` directory.  
Reset all gates: stop server, move/delete the directory (irreversible).

## Basic Usage
### Manual Standard Gate (2D example)
1. Build Standard frame (Obsidian by default) per shape file.
2. Place lever/button on the DHD (activation block) facing outward.
3. Right-click the lever. If valid: “Valid Stargate Design”.
4. `/wxcomplete <GateName> [idc=CODE] [net=Network]` to finalize.
5. Activate with lever; dial via `/dial <OtherGateName>` or sign cycle.

### Quick Auto-Build
1. Stand where DHD should be.
2. `/wxbuild Standard` (or another shape).
3. Right-click lever.
4. `/wxcomplete <Name>`.

## Core Commands (Short Form)
| Command | Purpose |
|---------|---------|
| `/wxbuild <Shape>` | Begin validation/build for a shape |
| `/wxcomplete <Name> [idc=CODE] [net=Net]` | Finalize pending gate |
| `/dial <Gate> [IDC]` | Dial a target gate (IDC unlocks iris) |
| `/wxlist` | List gates |
| `/wxremove <Gate> [-all]` | Remove gate (optionally destroy blocks) |
| `/wxforce <Gate|-all>` | Force shutdown/darken/open iris |
| `/wxgo <Gate>` | Direct teleport (permission) |
| `/wxcompass` | Point compass to nearest gate |

## Permissions (Modern Simplified)
Provided in `plugin.yml`:
- `wormhole.build` – Build & complete gates
- `wormhole.use` – Use /dial, list, compass, walk through
- `wormhole.admin` – Admin / config / force operations
- `wormhole.bypass` – Bypass restrictions & cooldowns
- `wormhole.*` – All of the above

Vault (if present) or built‑in fallback enforces nodes.

## Shape Files
Location: `plugins/WormholeXTreme/GateShapes/`  
Edit/add `.shape` files (2D & 3D). Changes load on restart. If folder is empty, defaults are auto-copied.  
Updated distribution uses WATER instead of STATIONARY_WATER.

## Troubleshooting
| Symptom | Check |
|---------|-------|
| Gate not recognized | Lever placement & frame materials, compare with shape file |
| No shape list | Using 1.0.4+? Earlier startup errors? Folder readable? |
| Gates not persisting | Confirm `.script` file after clean shutdown |
| Legacy material errors | Update to >= 1.0.3 (legacy mapping) |

Raise log detail: set log level to FINE in `config.yml` temporarily for deeper detection logs.

## Building From Source
```bash
mvn clean package
```
Result: `target/WormholeXTreme-<version>.jar` (shaded, deployable).

## Versioning & Changelog
See `CHANGELOG.txt` for detailed entries. 1.0.x = modernization + stability.

## License
GNU GPL v3 (see `LICENSE.txt`).

## Credits
Original authors: Lologarithm, alron. Modernization & maintenance by community contributors.

## Advanced / Legacy Details (Appendix)
### Additional Permission Nodes
- `wormhole.use.sign` – Use sign-dial gates
- `wormhole.use.dialer` – Use `/dial` lever mode
- `wormhole.use.compass` – Use `/wxcompass`
- `wormhole.remove.own` / `wormhole.remove.all`
- `wormhole.config` – Adjust materials/timeouts
- `wormhole.list` – Explicit list usage (normally covered)
- `wormhole.network.use.<NETWORK>`
- `wormhole.network.build.<NETWORK>`
- `wormhole.go` – Use `/wxgo`
- `wormhole.build.groupone|grouptwo|groupthree` (aliases group1/2/3)
- `wormhole.cooldown.groupone|grouptwo|groupthree`

If you only need simple control, stick with the four main nodes.

### Shape File Legend
Inside a `GateShape=` block:
- `[O]` Frame / structure
- `[P]` Portal cavity (becomes portal / iris material when active)
- `[O:L]` Lit frame block when gate active
- `[O:S]` Dial sign mount
- `[O:E]` Exit reference for player teleport positioning

Offsets: `BUTTON_UP`, `BUTTON_RIGHT`, `BUTTON_AWAY` determine required lever/button relative placement to detect a frame.

### Pre‑1.0.0 Migration Notes
- `Settings.txt` auto-migrated to `config.yml` once; then only `config.yml` is read.
- Legacy STATIONARY_* / PORTAL names mapped to modern enums automatically.
- Old per-gate numeric custom materials ignored (shape defaults used).

### Removing Legacy `Settings.txt`
1. Stop server.
2. Delete `plugins/WormholeXTreme/Settings.txt` (and any `Settings.old`).
3. Start server – the plugin uses only `config.yml`.

Do **not** delete `WormholeXTremeDB/` unless wiping all gates.

### Backups
- Stop server first.
- Copy `WormholeXTremeDB/` and custom shape files.
- To revert a shape: restore the file and restart.

---
Enjoy exploring the stars (responsibly). ✨
