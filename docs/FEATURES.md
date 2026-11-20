# FMod features

All FMod modules are in the **FMod** category of Meteor's module menu. DubCounter, VolytraFly instruments, Weather and EntityList HUD elements are in the **FMod** HUD group. Commands use Meteor's command prefix, which is `.` by default.

Back to the [README](../README.md).

## Stash hunting, travel and alerts

These modules are ported from the `26.1.2` branch of [miles352/meteor-stashhunting-addon](https://github.com/miles352/meteor-stashhunting-addon). See [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) for their source and authors.

| Modules | What they do |
| --- | --- |
| BetterStashFinder, OldChunkNotifier | Find storage blocks in loaded chunks, filter by old chunks, save stash records as CSV and JSON files, add map markers, and send optional webhook alerts. |
| TrailFollower, TrailMaker, SearchArea, GotoPosition | Follow old-chunk trails, record routes from XaeroPlus drawing highlights, resume rectangle or spiral searches, and steer toward a destination. |
| AutoLogPlus, DiscordNotifs | Log out on more triggers, and send notifications to your own Discord webhook. Webhook URLs are empty by default. |
| HighlightOldLava, VanityESP | Highlight old flowed lava, map art and banners. |
| AutoPortal, AutoEXPPlus, NoJumpDelay | Build portals, repair equipment with experience, and remove the jump cooldown. |

TrailFollower supports two mutually exclusive controllers for yaw-based following. **meteor-elytra-fly** (on by default) uses Meteor's built-in ElytraFly; equip an elytra and start gliding. **meteor-flight** uses Meteor's normal **Flight** module and needs no elytra or gliding. Enabling either option turns the other option off. Your selected Meteor module's mode and settings are preserved. TrailFollower holds forward while that controller is active and no screen is open, then restores the current physical key state on release. It turns the controller off on stop only if it enabled it; an already active module or one you manually re-enable remains under your control. It does not start both Meteor flight controllers together. Turn both options off to manage flight yourself. Nether Baritone following is unchanged; choose Advanced **flight-method: YAWLOCK** to use Meteor Flight there instead.

FMod adds **no-unloaded-chunks** (on by default) to Meteor's normal **Flight → General** settings. It pauses horizontal movement before the player's swept body crosses into a chunk the client has not received, including chunks between the start and end of a fast move. Flight stays enabled and resumes as chunks load; vertical movement remains available. This applies to manual Flight and TrailFollower's **meteor-flight** option, with no optional-mod requirement for the guard itself. Existing unloaded overlaps can be exited, and extremely large movement sweeps are rejected to bound work per move. It does not guard teleports, vehicles, other flight modules, or server corrections.

Better Stash Finder can also open a local stash map for browsing saved results. Open it with the module button or `.stash-map`.

## Optional mods

Some modules need other mods. FMod still loads without them, and the modules that need them do not appear.

| Module | Needs |
| --- | --- |
| BetterStashFinder, OldChunkNotifier, TrailMaker | Xaero's World Map, Xaero's Minimap, and XaeroPlus |
| TrailFollower | Xaero's World Map, XaeroPlus, and Baritone |

The port was tested at startup with these versions for Minecraft 26.3:

| Optional mod | Version |
| --- | --- |
| Xaero's World Map | `fabric-26.3-1.47.2` |
| Xaero's Minimap | `fabric-26.3-26.6.2` |
| XaeroPlus | `2.37.2+fabric-26.3` |
| Meteor Baritone | `26.3-SNAPSHOT` |
| Fabric API (required by Xaero's maps) | `0.164.0+26.3` |

## Stash mover

**stash-mover** automates account **A** as a shulker courier. The former **stasis-courier** name still resolves to this module. The courier loads at the source, waits for an operator to recall it to the destination, unloads, and returns empty. Two empty round trips must pass before it will load cargo.

### Return modes

Choose the module's **return-mode** for your server.

- **KillCommand** is the default and uses **`/kill` and the source bed**. This is the 6b6t route when the server advertises the command. Set and mark A's Overworld bed, and keep both halves and the respawn space clear. For a Stasis outbound recall, the destination pearl must survive A's death. Vanilla removes owned pearls on death by default. If outbound stasis fails on 6b6t, choose **Tpa** and enter B's Java username. The [official 6b6t command list](https://www.6b6t.org/en/commands) documents the server commands.
- **TrapButton** uses the source bed and a vanilla redstone death trap at the destination. Put a resettable trap under or around the landing spot, such as a button-operated floor over lava. Keep the chamber and storage clear of the trap. Look at its button and use **Mark destination death trap button**. It must be visible, unpowered and within reach from the landing spot. A presses it once after confirming the shipment is stored and A is empty, then waits for death and verifies the bed respawn. The trap must reset before the next arrival. For a Stasis outbound recall, the server must preserve A's pearls on death. With default vanilla pearl behavior, use **ReturnPearl** for a complete two-way stasis route.
- **ReturnPearl** works without commands when paired with **Stasis**. Build a stasis chamber at each station in the Overworld. B triggers A's destination pearl when the module says **B: recall**. A source operator triggers A's source pearl when it says **Source: recall EMPTY courier**. Each operator must be able to reach their chamber in time. Vanilla pearl teleports can deal five damage, so keep A healthy and give natural regeneration time between trips.

### Setup and operation

1. Mark a reachable chest or barrel at each station. Source storage must contain filled shulker boxes. Supply ender pearls in each station where A needs to arm a chamber. Ender Chest cargo mode also needs a reachable Ender Chest at each station.
2. At each pearl station, stand at the intended arrival position, aim into the chamber, and capture that station's landing and throw pose. Mark a block near the resting pearl for **chamber-radius**. Re-arming requires landing within 1.5 blocks of the captured pose. A newly thrown, owned pearl must stay observed in the region for 20 entity ticks.
3. With **KillCommand** or **TrapButton**, start empty at the destination and press **Start two EMPTY round trips**. With **ReturnPearl**, manually pre-arm A's first destination pearl and bring A to the source alive and empty before starting. The module arms the source pearl before the first outbound recall, then rearms the station it is about to leave on each leg.
4. After both round trips finish at source, press **Start cargo run from source**. **shulkers-per-shipment** limits each batch and **shipments** limits the number of deliveries. The run ends with A back at source. **EnderChest** mode verifies the shipment in A's private Ender Chest, withdraws the matching contents at destination and preserves the preexisting contents. **Carried** mode moves the shipment in A's inventory.

**Tpa** sends one `/tpa <B>` per outbound recall and waits for observed arrival. B accepts manually. **recall-timeout-seconds** bounds waits in both directions. Before an empty return, the module verifies the shipment was stored and A's inventory, equipment, crafting grid and cursor are empty with the container closed.

**STOP automation** cancels further actions and keeps session setup. A trap already triggered follows its own redstone and block behavior. Already completed transfers stay in their containers. A timeout, missing pearl, failed transfer or unexpected inventory stops the run. Inspect both inventories and chambers before restarting a stopped route. Disabling, joining or disconnecting clears station marks and trial results. Changing a route setting invalidates its trials.

**Container areas** optionally replace the single cargo chest with a bounded source and destination selection. Look at each corner and use `.sm input1`, `.sm input2`, `.sm output1`, `.sm output2`, then enable **container-areas**. Each area must be in its station's dimension, at most 64 blocks per axis and 32,768 blocks total. Only received, reachable chests/barrels participate; the courier stays still. Batches can span sources and spill into another destination when one fills. Each destination receipt is verified before the remaining manifest shrinks. Keep the original station/supply and Ender Chest marks configured. The module never breaks empty chests or walks an area. `.sm test`, `.sm start`, `.sm status` and `.sm stop` mirror the main controls.

## Pearl operator

**pearl-loader** runs on B at the destination. Configure a chamber name, look at its reachable trapdoor and use `.pl mark`; `.pl trigger [name]` performs a close/hold/open cycle using normal block interaction and server block-state acknowledgements. Use an empty main hand, empty cursor and closed inventory. It does not walk, build a chamber, keep a region loaded while B is absent or prove A arrived. Check the chamber manually after cancelling a cycle, which leaves the last confirmed door state. Named chamber positions are saved privately per server.

B remains manual by default. **signed-chat-triggers** is an optional operator mode: whitelist courier UUIDs and accept only an exact signed player message `!pearl <name>` (keyword configurable). System messages, unsigned text, formatted names and server whispers cannot authorize it. Server chat transformations may therefore make this mode unavailable on 6b6t. StashMover sends no automatic trigger messages. No Baritone or anti-AFK movement is included.

## VolytraFly

**volytra-fly** ports [Volizray's VolytraFly](https://github.com/Volizray/VolytraFly-Addon), revision `809295685d76eb03e77f6c270ff2d45bd78666e2`, into the FMod category. Equip an elytra, disable other flight controllers, enable the module and start gliding. Meteor's ElytraFly, Flight and PacketFly cannot run alongside it; enabling a conflicting controller stops VolytraFly. TrailFollower's default Meteor-flight option also conflicts, so stop TrailFollower before using this flight module independently.

The upstream defaults start horizontal motion at **2.999 blocks/tick**, ramp toward **14.999 blocks/tick**, use a **14.999 plateau** and a **0.3 acceleration step**. The gain tapers as speed approaches the plateau. At 20 client ticks/s the requested ceiling is **299.98 blocks/s**; neither the setting nor a successful local test establishes what live 6b6t accepts. Movement corrections are applied normally and rewind the ramp. Repeated corrections near the configured threshold temporarily reduce its cap.

Assign the main on/off bind through Meteor's normal module binding control. The **Keybinds** group contains independent shortcuts for the modes and cancelling Autonav. They support keyboard keys, keyboard modifiers and mouse buttons through Meteor's binding system, and activate once on release. Mode shortcuts require VolytraFly enabled and a live player in a world; chat, menus and paused screens leave modes unchanged. Existing saved bindings are preserved. The main bind still honours the flight-controller conflict checks above.

The port includes vertical controls, additive **Bypass Mode**, collision/height/unloaded-chunk guards, **Mapping**, **Building Mode**, landing slowdown, player/projectile avoidance, elytra replacement, firework replenishment, autopilot, highway steering and Autonav. Optional groups start collapsed. **Overdrive** is off by default; it runs extra local-player steps while ramping, bounded by the configured horizontal distance per game tick. Its extra position packets are suppressed in singleplayer as upstream requires. This is separate from Meteor's Timer.

Use the **Visuals** group for the speedometer, fuel gauge, mode lights, assistant, Future Sight and route display. Their HUD elements belong to FMod and are placed on activation when enabled. Enable Meteor's global HUD if it is off; existing HUD elements are preserved. Disabled instruments skip text layout and drawing after their short power-off animation; the HUD editor still shows previews. FMod's speedometer measures net displacement over real elapsed time instead of displaying intended speed after a correction. Corrections and world changes reset that sample. It remains a client observation, not a server acceptance guarantee.

Autonav accepts typed X/Z in its settings and a **Go** button. Xaero's World Map is optional: select a location in the **current dimension's** map with the configured click button. Unsupported map fields or a different dimension reject the click. Navigation uses received terrain; missing chunks can pause or limit progress. Runtime destinations/routes clear on disconnect or disable, while typed coordinate settings persist normally. Coord Hider suppresses supported assistant destination labels; map terrain, individual settings fields and route geometry can still disclose locations.

While Autonav is enabled **and controlling a route**, **No Crash** is temporarily bypassed so its straight-ahead ray cannot block planned detours. Your No Crash preference takes effect again when navigation stops; Autonav keeps its own body-clearance checks. The planner checks the swept hitbox between cells, follows only currently clear route segments, and replans when a passage becomes blocked. Stair and other partial-block collision shapes are checked by their individual parts. Tight detours slow to at most two blocks/tick (40 blocks/s at 20 ticks/s), while open travel keeps the configured flight ceiling. Searches and visibility lookahead remain bounded and need no installed Baritone.

**arrival-radius** defines an acceptable stopping region (30 blocks by default). Once inside it, Autonav hovers at the point it reached; it does not force a final straight flight through intervening terrain to the exact centre. Reduce the radius for a closer approach. Sealed areas, missing terrain and passages the player's body cannot fit still require waiting or manual intervention.

Nether wall recovery starts its search at your actual position, including the first swept movement and arrival-distance check, rather than snapping to the middle of your block. After four active seconds without meaningful movement, it cancels the stalled search and tries a short, verified local escape before retrying the destination. Small wall jitter and repeated replanning do not restart the movement clock. Recovery and pause messages use local Meteor chat, so the HUD can stay off. If it cannot find a checked escape, or cannot clear the stalled area within 20 seconds of recovery, it pauses the route, brakes and holds position while gliding instead of retrying forever. **Go** retries; the cancel bind or movement keys return control. Intentional game pauses do not consume that recovery budget.

**Autonav diagnostics** are on by default. When stuck, run **`.vnav snapshot`**, then attach the printed `.jsonl` file when reporting the problem. It captures nearby block states as well as route decisions, search results/timing, movement clipping, corrections, recovery and pause state. **`.vnav log`** shows file locations, **`.vnav log copy`** copies the path, and **`.vnav log off`** / **`on`** control recording. A snapshot button is also in the diagnostics settings group. Recent incident logs remain available after stopping or retrying a route; capture them before later activity rotates them away.

Logs stay in the active game directory's `meteor-client/fmod-private/autonav-diagnostics/`, with at most four rolling files plus one saved snapshot, each limited to 2 MiB. `autonav-snapshot.jsonl` stays unchanged until your next snapshot request. Summaries are sampled about once a second, repeated search events are limited, and a bounded worker performs serialization and disk writes. Relative positions, block fractions and local terrain offsets are the default; server addresses, player identity, chat, inventory contents and block-entity data are excluded. **diagnostic-absolute-coordinates** explicitly enables real locations. Coord Hider does not redact files. Terrain layouts may still identify a build, so share diagnostic files privately. Nothing uploads automatically, and logs remain excluded from Git and release artifacts. Turning logging off stops new capture; already accepted writes may finish.

Disabling releases simulated input, cancels navigation and shuts down its bounded workers. Death stops the module too. Inventory helpers pause while a screen/container or cursor stack is in use, and an active StashMover run prevents flight control. FMod omits upstream's delayed post-disable insta-drop and wait-for-landing chest-swap hooks so stopping leaves no queued flight/inventory actions. **Always** chest swap retains its immediate on/off behavior. Speeds are bounded to 32 blocks/tick and plateaus to 64; invalid non-finite settings stop flight. No optional mod is required for basic flight.

## Container tools

These are native Minecraft 26.3 adaptations of the [BepHax feature set](https://github.com/dekrom/BepHaxAddon); they work without optional map mods. ChestIndex uses an original FMod implementation sharing ChestTracker's backend.

- **chest-tracker** records storage you open while enabled. `.ct` opens its searchable browser; `.ct search <text>` matches item identifiers and custom names, including nested shulker contents. Snapshots show their observation time and verification state. Predicted changes after a click remain unverified until the container is reopened. Ender Chest recording is off by default and is account-specific when enabled.
- **chest-index** automatically opens unrecorded storage within normal interaction reach. ChestTracker's **auto-open** option uses this same single scanner. It does not walk, load distant terrain or take over user-opened menus. Auto-opening pauses whenever StashMover is enabled, including between runs; passive recording continues. `.ct rescan` allows another inspection of reachable containers. Neither module can know unopened inventories from storage ESP alone.
- **shulker-overview** adds the most common contained item's icon to shulker item slots and a `+` for mixed contents. Empty boxes have no overlay; replacing the contents component refreshes the preview.
- **DubCounter HUD** counts selected storage in received chunks, separates single/double/partially received chests, shows shulker colours and optional slot capacity. Place it from Meteor's **FMod** HUD group. An incomplete double contributes only its received half; Ender Chests contribute no ordinary storage capacity. This counts blocks/slots, not their contents or nested capacity. Coverage remains limited to the client's loaded chunks.

Tracker records contain real coordinates and item data. Container records are separated by server and account, with up to 10,000 entries. Sign history and chamber marks are separated by server. These files live under `meteor-client/fmod-private/` in the active game directory; they are ignored by Git and excluded from the JAR. Coord Hider protects supported browser labels, not these files. Do not share them in configuration bundles. Accepted writes may finish after a module stops; no new gameplay actions continue after cancellation.

## Signs, branding and travel

**sign-historian** privately records received sign text, colour, glow and wax state, retaining the first observation and up to seven later versions for at most 5,000 locations. `.signhistory` opens the history browser. Markers show observed changes/removal without replacing live world blocks. Observation times do not establish creation dates or authors. **restore-on-edit** is off by default; when enabled it fills an authorized sign editor with the earliest observed text, taking priority over SignatureSign. Confirm normally to send it. It does not replace sign blocks, remove wax or automatically restore styles.

**signature-sign** fills an authorized editor with four independently configured lines per side. Existing text is preserved by default; overwriting and automatic confirmation are separate opt-ins. Templates include custom/empty text, username, timestamp/epoch, brand, UUID variants, Base64, hex, ROT13 and local-file lines. File loading is bounded to 1 MiB/8,192 lines; invalid templates abort without partially applying the template. Text is fitted to the sign's native line width.

**stash-brander** bulk-renames selected inventory items through an open anvil. Configure an item whitelist and **custom-name** first; the empty default whitelist changes nothing. Existing names are skipped unless **rename-named** is enabled. **maximum-level-cost** defaults to one level per operation. It requires the server's matching output and cost before taking the result, and stops on timeout or manual interference. Inspect retained anvil inputs after stopping.

**road-trip** monitors horizontal distance, average real-time speed and target ETA without steering. Use `.rt start`, `.rt target <x> <z>`, `.rt status` and `.rt stop`. Targets apply to the current server/dimension and must be reapplied after context changes. It warns about low equipped-elytra durability, missing packets and stalled travel. Optional arrival, timer, minimum-Y, supply and stall logout guards are all disabled by default. Logout is a normal disconnect and disables Meteor AutoReconnect; it sends no malformed kick packets. Counts cover loose inventory supplies, not items packed in shulkers.

## Coordinate hider

Turn on **coord-hider** before recording or streaming, then choose a **mode**:

- **Hide** (default) suppresses F3 location entries and position HUD elements, and redacts recognised coordinate text.
- **Randomize** uses one synthetic XYZ location generated by Java's `SecureRandom`, without reading or offsetting your real position. The values stay fixed during the session, including while you move. Activation, a world join, changing to Randomize, or **Regenerate fake coordinates** creates a fresh location. The values and generator state are not settings and are not saved.

Both modes use the existing coverage switches:

- **hide-f3-location** protects F3 location entries. Randomize shows synthetic XYZ, block and derived chunk coordinates. Targeted block/fluid/entity, section and terrain-detail entries remain hidden.
- **hide-meteor-coordinate-hud** protects Meteor position expressions, including split axes, camera/target positions and opposite-dimension coordinates. Recognised position reads are replaced before the HUD script runs. Coordinate templates with unsupported syntax are suppressed. Regeneration or mode changes refresh the cached HUD immediately.
- **mask-coordinate-text** protects recognised triples, labelled axes and stash coordinate pairs in vanilla and Meteor text. Supported formats share the same synthetic location. Ordinary non-location text is preserved.

This is display protection, not complete location anonymity. The module does not change your player position, movement packets, route data or global Starscript values. Exports, logs, clipboard content and configured webhook messages keep real coordinates. Maps, world geometry, individual input fields and overlays using unrecognised formats can still reveal your location. Choose Hide if you prefer to suppress coordinates instead of displaying a fixed synthetic location.

## UI Utils

Enable **ui-utils** in the FMod category, open an inventory or container, and select **UI Utils** in the top-left corner. This is a native Minecraft 26.3 implementation of [Coderx-Gamer/ui-utils](https://github.com/Coderx-Gamer/ui-utils)'s debugging workflow. It needs no optional mods or separate UI-Utils installation. The packet editor runs inside Minecraft instead of opening a desktop window.

- **Close without packet** hides the screen while retaining the client menu and leaving the server container open. **De-sync** submits a container-close packet while retaining the client menu.
- **Send packets** blocks or permits container slot/button packets. Turning it off also discards delayed packets. **Delay packets** holds these packets in FIFO order; turning delay off flushes them. The queue holds at most **512** packets; further clicks are discarded with a warning. You can also flush or discard explicitly.
- **Save GUI** retains one container screen in memory. Restore it with **V** by default (configurable as **restore-screen**) or `.ui-utils restore`. This deliberately reattaches the saved client menu; the server may have closed or replaced it. Saved screens and queued packets are cleared on disconnect, reconnect, or module disable and are never written to disk. Disabling never flushes packets.
- **Fabricate packet** edits the sync ID, revision, slot, button, action and repeat count for container-click or container-button packets. Repeats are limited to **1–100** per submission and numeric fields must fit their wire types. Click packets use an empty changed-slot map and the current carried-stack hash without predicting local inventory changes. **Respect delay** controls queueing; sending off still blocks fabrication. Submission is not confirmation that a server accepted an action.
- **Send queued + disconnect** submits the queue then disconnects immediately. Server processing and any race-dependent effects are not guaranteed. **Copy GUI title JSON** copies the original component, including formatting, to the clipboard.
- Inventories, book screens and sign editors have a chat/command field. Focus it and press **Enter** to send; **Escape** releases its focus. `^toggleuiutils` or `.ui-utils controls` toggles the added controls. Sign editors also offer close without an update and disconnect buttons. The sleeping chat screen offers a client-only wake button.
- On the multiplayer screen, **Bypass packs** acknowledges required resource-pack offers without downloading them. **Force deny** extends that behavior to optional packs while bypass is on. Both default off; the module must be enabled. Responses use the offered pack's UUID. This cannot guarantee that a server will allow you to remain connected.

## Other modules

- **EventlessFly** (`eventless-fly`) applies configurable directional flight after Meteor's movement event. It uses standard Minecraft 26.3 position updates and pauses for competing flight controllers and Stash Mover.
- **KnockbackTweaks** (`knockback-tweaks`) changes the attack yaw for knockback that uses the attacker's facing. Choose a direction relative to the target, relative to your view, or absolute. Optional random spread and sprint knockback are available.
- **SeedMap** (`seed-map`) follows your position in [Chunkbase Seed Map](https://www.chunkbase.com/apps/seed-map). Enable the module, open the map, select the seed and version, then use **Copy browser script** and run it in that tab's developer console. Allow local-network access if prompted. The script reads coordinates from a session-specific loopback URL and recenters the map. Updates pause while Coord Hider is active. Disable the module to close the connection or run `fmodStopSeedMap()` in the tab to stop following. Copy a fresh script after re-enabling the module.

- **InstaMine** attempts to finish mining immediately when your current tool and effects give more than `0.5` block-breaking progress per tick, matching FMod's previous threshold. It sends sequenced start/stop mining actions and predicts the local removal; the server can reject or correct the break. Slower blocks use normal mining. Creative mode, game-mode/tool restrictions, air and unbreakable blocks keep normal behaviour. No optional mods are required.
- **InfReach** (`inf-reach`) uses the shared warp helper for long-range entity interactions.
- **AntiMapCrash** (`anti-map-crash`) filters incoming map updates based on its settings.
- **NoFpsThrottle** (`no-fps-throttle`) replaces vanilla's minimized/AFK frame-rate throttle (10 FPS minimized, 30 then 10 FPS when idle under the `AFK` inactivity setting) with `limit`, which never exceeds the Max Framerate video setting (260 = use it unchanged). The client runs at most 10 ticks per frame, so a heavily loaded throttled client drops below 20 TPS; this keeps background Baritone clients, which send no input and look idle, ticking at full speed. It does not raise FPS lost to CPU/GPU load or to OS/driver background limits.
- **SpeedBypass** (`speed-bypass`) pads position-less `StatusOnly` packets so the server accepts faster movement, within the vanilla per-tick ceiling. The server resets the move-packet count to 1 once it passes 5, so the usable cap is five packets per tick, roughly **22 blocks/tick walking/flying, ~38 blocks/tick on elytra**. **Dynamic** mode pads from your actual speed. **Static** pads a fixed amount. `boost` optionally adds horizontal blocks/tick in your movement direction, clamped under that ceiling. With `boost` at zero, speed comes from elytra, another module or velocity.
- **NoTextureRotations** stops the random rotation of some block textures, such as grass, sand and bedrock. Rejoin the world or reload graphics after you turn it on. Based on [rfresh2/NoTextureRotations](https://github.com/rfresh2/NoTextureRotations/).
- **FrameDupe** places and breaks items in nearby item frames in a fast loop. Some servers have duplication bugs that this can trigger. Hold the desired item when enabling. It fills empty frames and only attacks a frame after observing the matching item and components inside. Range, frame order and each delay are settings.

## Commands

The movement commands are based on [JorianWoltjer/LiveOverflowMod](https://github.com/JorianWoltjer/LiveOverflowMod).

The clip/teleport commands and InfReach share a single warp helper tuned for vanilla 26.3. Vanilla disconnects a client that sends more than one positioned move packet per tick and caps the per-tick speed allowance at roughly 22 blocks (five move packets; sending more resets the allowance), so each warp sends one padded positioned packet per tick and the helper suppresses the client's own position packet while warping. Longer moves are split into ~20-block steps across ticks. Vertical movement clips through blocks; horizontal movement is collision-checked and snaps back. Servers with anti-cheat beyond vanilla may still reject these.

| Command | Aliases | What it does |
| --- | --- | --- |
| `.stashmover <action>` | `.sm` | Courier trials, shipments, area corners, status and stop. |
| `.pearl-loader <action>` | `.pl`, `.pearlloader` | Mark and manually trigger a named operator trapdoor. |
| `.chest-tracker [search <text>]` | `.ct`, `.chestindex` | Browse/search container records; also `status` and `rescan`. |
| `.ui-utils [action]` | `.uiutils` | `restore`, `clear`, `flush`, `controls`, or `status` for the container debugging tools. |
| `.signhistory` | `.shistory` | Browse observed sign history. |
| `.roadtrip <action>` | `.rt` | Travel monitoring and current-dimension targets. |
| `.volytra-nav snapshot` / `log` | `.vnav` | Capture local Autonav diagnostics, locate/copy the log, or switch recording on/off. |
| `.teleport <x> <y> <z> <ticks>` | `.tp` | Warps to a position using bounded steps. The legacy ticks argument is accepted for compatibility. |
| `.autoclip <direction>` | `.ac`, `.aclip` | Moves you up (`1`) or down (`-1`) through blocks to the nearest air space. |
| `.shclip <distance>` | `.hc`, `.shc` | Moves you horizontally through walls in the direction you are looking. |
| `.dclip <distance>` | `.dc` | Moves you in the exact direction you are looking, including up, down or diagonally. |
| `.fvclip <blocks>` | `.vc` | Clips vertically by the requested number of blocks. |
| `.stash-map` | `.stashmap`, `.smap` | Opens the local stash map and analysis. |
