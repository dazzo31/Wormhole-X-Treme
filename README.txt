Wormhole X-Treme v1.0.4 (Modernized)
====================================
NOTE: Canonical maintained documentation has moved to README.md. This README.txt is retained temporarily for historical reference and will be removed in a future release.

Travel instantly between locations using buildable Stargates with dial signs, activation levers, iris security, networks, and configurable materials.

Key Modernization (1.0.x line)
------------------------------
* Updated for Java 21 and Spigot/Paper 1.20.4+ (tested on 1.21.x).
* Shaded single-jar deployment (no extra lib folder needed).
* Embedded file-based HSQLDB (no external DB required) stored in `plugins/WormholeXTreme/WormholeXTremeDB`.
* Config migrates to `config.yml` (legacy `Settings.txt` auto-converted on first run if present).
* Legacy shape material names (STATIONARY_WATER, STATIONARY_LAVA, PORTAL, GLOWING_REDSTONE_ORE) auto-mapped.
* INFO level startup summary lists loaded gate shapes.

Requirements
------------
* Java 21 JVM
* Spigot or Paper 1.20.4 or newer (1.21+ recommended)
* (Optional) Vault for advanced permissions/economy integration

Installation (Fresh)
--------------------
1. Download the latest `WormholeXTreme-<version>.jar` from releases.
2. Place it in your server `plugins/` directory.
3. Start the server. Folder structure and default shapes are created automatically.
4. (Optional) Stop server and adjust `plugins/WormholeXTreme/config.yml` (timeouts, permissions mode, cooldowns, etc.).
5. Start server again – you should see: `Loaded N stargate shape(s): [...]` in console.

Upgrade
-------
1. Back up `plugins/WormholeXTreme/` (especially the `WormholeXTremeDB` folder and custom shapes).
2. Replace old jar with new jar.
3. Start server (automatic DB migrations + config migration if coming from pre‑1.0.0 legacy).
4. Verify shapes list and gate functionality. No manual SQL changes needed.

Data & Persistence
------------------
The embedded HSQLDB stores data in `plugins/WormholeXTreme/WormholeXTremeDB`:
* `WormholeXTremeDB.script` – schema & data snapshot (safe to back up while server is stopped).
* `*.lck` / `*.log` – transient while running.
To back up: stop server, copy the entire directory. To reset: stop server and delete the directory (all gates lost).

Basic Usage
-----------
Manual Standard Gate (2D example):
1. Build the Standard frame from obsidian (or shape file material).
2. Place a lever (or button) on the DHD block (front-center lower portion per shape file) facing outward.
3. Right-click the lever. If valid you’ll see a “Valid Stargate Design” message.
4. Run `/wxcomplete <GateName> [idc=CODE] [net=Network]` to finalize.
5. Activate with the lever; dial using `/dial <OtherGateName>` or by cycling a sign gate’s dial sign.

Quick Auto-Build:
* Stand where you want the DHD/lever.
* Run `/wxbuild Standard` (or another listed shape) then right-click the placed lever – finish with `/wxcomplete`.

Core Commands (Short Form)
--------------------------
* `/wxbuild <Shape>` – Prepare to validate/build a frame at your lever location.
* `/wxcomplete <Name> [idc=CODE] [net=Network]` – Finalize a pending gate.
* `/dial <GateName> [IDC]` – Dial a target (IDC unlocks remote iris if set).
* `/wxlist` – List known gates.
* `/wxremove <GateName> [-all]` – Remove a gate (optionally destroy blocks).
* `/wxforce <Gate| -all>` – Force shutdown/darken/open iris.
* `/wxgo <GateName>` – Teleport directly (permission gated).
* `/wxcompass` – Point compass at nearest gate.

Current Permissions (Simplified)
--------------------------------
Default nodes (plugin.yml):
* `wormhole.build` – Build & complete gates.
* `wormhole.use` – Use /dial, walk through, list, compass.
* `wormhole.admin` – Administrative commands (/wxforce, /wormhole config operations).
* `wormhole.bypass` – Bypass restrictions/cooldowns.
Composite: `wormhole.*` grants all. Vault (if installed) or built‑in mode handles checks.

Legacy fine-grained nodes (sign vs dialer, networks, groups) are internally mapped; modern setups typically only need the four above. If migrating from very old installs you can safely ignore obsolete SIMPLE/COMPLEX permission toggles—current config keys cover built‑in vs Vault usage.

Shape Files
-----------
Location: `plugins/WormholeXTreme/GateShapes/`
Edit or add `.shape` files (2D & 3D). On restart the plugin parses and lists them. If no shapes are found, a starter set is auto-copied.

Troubleshooting
---------------
* Gate not recognized: confirm lever is on correct activation block & frame matches shape materials; check console for “Shape: <name> is found!” lines if log level raised to FINE.
* No shape list at startup: ensure you are on 1.0.4+; check that plugin loaded (no earlier errors); delete empty GateShapes folder to trigger default copy.
* No gates persisting: verify DB directory contains `.script` file after clean shutdown; avoid force-killing server.
* Legacy material errors: update to 1.0.3+ (mapping added); shape distribution uses WATER now.

Building From Source
--------------------
Prerequisites: JDK 21 + Maven 3.9+
Steps:
1. Clone repo.
2. `mvn clean package` (tests optional; `-DskipTests` to skip).
3. Output jar: `target/WormholeXTreme-<version>.jar` (shaded, ready to drop in).

Versioning & Releases
---------------------
* Semantic-ish: 1.0.x = modernization + stability fixes.
* Changelog: see `CHANGELOG.txt` for detailed entries and upgrade guidance.

License
-------
GNU GPL v3 (see `LICENSE.txt`).

Credits
-------
Original authors: Lologarithm, alron. Modernization & maintenance contributors – see project history.

Enjoy exploring the stars (responsibly). ✨

Advanced / Legacy Details
-------------------------
This appendix preserves still-supported legacy behaviors and nodes not listed in the main quick start.

Additional Permission Nodes (legacy granularity)
* wormhole.use.sign                – Use sign‑dial gates (subset of wormhole.use)
* wormhole.use.dialer              – Use /dial lever (subset of wormhole.use)
* wormhole.use.compass             – Use /wxcompass
* wormhole.remove.own              – Remove only gates you own
* wormhole.remove.all              – Remove any gate
* wormhole.config                  – Adjust materials / timeouts (admin style)
* wormhole.list                    – Explicit list permission (normally covered by wormhole.use)
* wormhole.network.use.<NETWORK>   – Use gates on a specific network
* wormhole.network.build.<NETWORK> – Build gates on a network
* wormhole.go                      – Direct teleport via /wxgo
* wormhole.build.groupone|grouptwo|groupthree (or group1|group2|group3) – Build restriction tiers
* wormhole.cooldown.groupone|grouptwo|groupthree – Use cooldown tiers

If you only need simple control, stick with: wormhole.build, wormhole.use, wormhole.admin, wormhole.bypass.

Shape File Legend (Recap)
Within a .shape file GateShape= block:
 [O] Frame / structure material
 [P] Portal cavity (becomes portal/iris material when active)
 [:L] (O:L) A frame block that lights when active
 [:S] (O:S) Frame block holding the dial sign (optional)
 [:E] (O:E) Frame block whose relative position defines player exit origin
Coordinates & Offsets: BUTTON_UP / BUTTON_RIGHT / BUTTON_AWAY control where the lever/button must be relative to the lower reference corner so detection succeeds.

Pre‑1.0.0 Migration Notes
* Legacy Settings.txt is auto‑migrated to config.yml on first 1.0.x run.
* Legacy STATIONARY_* or PORTAL materials inside shapes are auto-mapped; you may safely replace them with WATER / LAVA / NETHER_PORTAL manually (bundled shapes already updated).
* Per‑gate custom materials stored as numeric IDs in very old data are ignored; gates adopt shape defaults.

Removing Legacy Settings.txt
After a successful migration (config.yml present, no errors) you can delete the old file:
 1. Stop the server.
 2. Delete plugins/WormholeXTreme/Settings.txt (and any Settings.old backups if you wish).
 3. Start the server – only config.yml will be used.
Do NOT delete the WormholeXTremeDB folder unless you intend to wipe all gates.

Backups & Safety
* Always stop the server before copying the WormholeXTremeDB directory or editing shape files.
* To revert shape edits: delete the edited file and copy from a clean release jar or backup.
* To reset all data: stop server, move (do not outright delete) WormholeXTremeDB folder elsewhere, then start.

Converting This README
If you prefer Markdown formatting on hosting platforms:
 1. Copy this README.txt to README.md.
 2. (Optional) Convert section underlines to Markdown headers (#, ##) for richer rendering.
 3. Commit README.md; you may keep or delete README.txt (project only needs one).

Deleting README.txt (if switching to README.md)
 1. Ensure README.md contains all content.
 2. git rm README.txt
 3. git add README.md && git commit -m "docs: convert README to Markdown".
 4. Push changes.

End of appendix.