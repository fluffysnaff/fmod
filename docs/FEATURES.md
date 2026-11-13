# FMod features

All FMod modules are in the **FMod** category of Meteor's module menu. BaseScout, DubCounter, VolytraFly instruments, Weather and EntityList HUD elements are in the **FMod** HUD group. Commands use Meteor's command prefix, which is `.` by default.

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

TrailFollower's **meteor-elytra-fly** option enables Meteor's built-in ElytraFly when it starts yaw-based following. Your configured ElytraFly mode and settings are preserved. Equip an elytra and start gliding; TrailFollower holds forward only while gliding with ElytraFly active and no screen open, then restores the current physical key state on release. It turns ElytraFly off on stop only if it enabled it; an already active module or one you manually re-enable remains under your control. Turn this option off to manage flight yourself. Nether Baritone following is unchanged.

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

**stash-mover** automates account **A** as a courier. The former **stasis-courier** name and saved settings resolve to this same module. Account **B** stays at the destination and triggers A's stasis chamber manually when A's status says ready. The loop is **source: load → B recalls A → destination: unload → rearm → `/kill` empty → bed respawn at source**. It sends no `/home` commands and does not build a physical route or move A between station blocks.

The route is a prerequisite, not a compatibility promise. Mojang documents pearls vanishing on owner death by default in [the death-rule documentation](https://www.minecraft.net/nb-no/article/minecraft-java-edition-1-20-2). [6b6t feedback issue #7](https://github.com/6b6t/feedback/issues/7) reports pearls disappearing on teleport. Neither establishes behaviour in your current session. The module requires **two empty death → bed respawn → actual destination recall trials** before enabling cargo runs. A visible pearl alone does not pass a trial. Run the trials with expendable pearls before committing cargo; changing the route or recall mode invalidates the results.

Enable the module and use its setup buttons while looking at the appropriate blocks:

1. **Source:** set A's respawn in an Overworld bed yourself, keep both bed halves and its respawn spaces intact, then mark the bed. Mark a source chest or barrel containing **filled shulker boxes**. Other source items and empty shulkers are ignored. Place station containers within eight blocks of the bed and within actual interaction reach from its respawn space; the module does not walk to them.
2. **Destination:** arrange a concealed chamber with a repeatable, safe landing/throw spot. Stand on that spot, aim as required to throw into the chamber, and capture the destination pose. Mark a block near where the waiting pearl will be visible; **chamber-radius** defines the region around that block. Mark a reachable destination chest or barrel, with spare ender pearls inside. A must land within 1.5 blocks of the captured throw pose to rearm automatically. The module throws A's pearl, checks that a newly observed owned pearl remains visible in the region for at least 20 entity ticks, and rechecks its presence before `/kill`. This observation does not prove the chamber is functional or that the pearl will survive death.
3. **Cargo protection:** **EnderChest** is the default cargo-storage mode. Mark A's source and destination Ender Chests too. The module puts the shipment into A's Ender Chest at source, then withdraws only matching shipment quantities at destination, preserving the previous contents. It stops if the expected Ender Chest contents change. **Carried** mode transports the shulkers in A's normal inventory instead. B cannot access A's Ender Chest inventory.
4. **Empty trials:** start at the destination with inventory, equipment, crafting grid and cursor empty, then press **Start two EMPTY trials from destination**. The module obtains a pearl from destination storage, rearms, stores remaining pearls, sends `/kill` once, and requests respawn after observing death. At source it checks for an empty, living courier beside the intact bed, then waits for B's recall. After two actual arrivals it rearms and returns A empty to source once more, leaving B waiting. Marking the bed does not set or guarantee its spawn; a blocked or wrong respawn stops progress.
5. **Shipments:** at source, press **Start cargo run from source**. **shulkers-per-shipment** limits each batch; **shipments** bounds the run (one by default). B should recall only when the module says **CARGO READY**; Meteor's module info also distinguishes cargo readiness from an empty trial. Transfers are verified from fresh server container snapshots after reopening, including destination receipt, before the module rearms and returns empty. The run ends back at source after the requested deliveries, or stops there if the source has no filled shulkers.

Keep A's armour, offhand and other equipment empty throughout. The final `/kill` gate checks carried inventory, equipment, crafting, cursor, closed container, destination, completed shipment and rearmed recall. The courier stores leftover pearls at destination before death. It does not throw away items or use death as the cargo-carrying leg. Normal death still affects experience. Server inventory changes after the final observation cannot be predicted by a client.

If stasis fails, select **Tpa** and enter B's exact Java username. A sends one `/tpa <B>` request per recall and waits for actual arrival; B accepts manually. Pearls and chamber marks are then unused, but the two empty bed-return trials are still required. This is an explicit alternative, not an automatic fallback. Server rejection or cooldown does not trigger request spam: **recall-timeout-seconds** stops the run. The [official 6b6t command list](https://www.6b6t.org/en/commands) documents `/kill`, `/tpa` and `/tpy`; their availability and timing must still work in the current session.

**STOP automation** cancels further actions and keeps session setup. Transfers already performed are not rolled back: inspect both inventories and the chamber before restarting. A full/unreachable container, unexpected contents, equipment, world/player change, unconfirmed pearl or timeout stops progress without advancing to death. Disabling, joining or disconnecting clears station locations and trial results; locations, manifests and results are never settings, exports, chat messages or webhooks. Filter settings and the Tpa operator name are saved normally. The module sends only its configured-route `/tpa` requests and empty `/kill` commands, and issues no further output after stopping.

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

## Base scouting

**base-scout** continuously inspects chunks your client receives and helps select a new Overworld base location. Its main controls are **Start scouting**, **Find another candidate**, and **Import exclusions**. It works without optional mods, moves no player, requests no missing or distant chunks, and sends no server chat, server commands, markers or webhooks.

For in-game control without opening a menu, use **`.bs start`**, **`.bs hud`** and **`.bs next`**. **`.bs`** or **`.bs status`** prints the current local report; **`.bs import`** opens the file picker; **`.bs stop`** stops scouting. **`.bs help`** lists the controls. `.base-scout` is the full command name, and all commands follow your configured Meteor command prefix.

Scouting automatically adds a **BaseScout HUD** if none is placed. It shows the live nearby result, coverage, storage/portal/construction/entity counts, XaeroPlus flags, private-list status, candidate and current operation. Text wraps onto a translucent background; move it or change scale/width in Meteor's **FMod** HUD group. Existing positions and disabled elements are preserved. If Meteor's HUD is globally off, the startup chat points to **`.bs hud`**: this explicitly enables Meteor's HUD system, including any other enabled HUD elements. **`.bs hud off`** hides only BaseScout, leaving other elements unchanged. The module's **show-hud** setting is on by default.

Local messages use Meteor's normal `Module.info()` / `Module.error()` pipeline, with the usual **[Meteor] [Base Scout]** prefixes (or your custom Meteor prefix). **chat-updates** is on by default. Automatic area reports combine changes and are limited to one per **chat-update-interval-seconds** (10 seconds by default); an unchanged area stays quiet. Import/load results and explicit status commands reply separately. Candidate coordinates remain on the HUD/menu and follow Coord Hider, rather than entering local chat logs. Private-list coordinates, file paths, authors and message history are never printed in the chat reports.

1. **Start scouting.** The nearby-area report follows your current chunk automatically. New chunk arrivals receive priority; a background sweep also notices changes in already loaded terrain. Inspection is bounded to one chunk per client tick and at most a 17-by-17 area. Missing and not-yet-inspected chunks produce **incomplete coverage**. Moving or changing dimensions discards observations outside the new area.
2. **Import exclusions.** Select coordinate JSON, a Discord Chat Exporter CSV/JSON, or coordinate text/HTML. Imports extract coordinates and dimensions, remove duplicates, and save only `x`, `z`, `dimension` and `radius`, with a file version. Authors, message IDs, dates, attachments and message history are discarded. Imports merge into the current server's private list; the raw export is never copied. Files are limited to 20 MiB and 100,000 coordinate records. Malformed imports preserve the saved list.
3. **Review ambiguity.** Missing or conflicting dimensions remain `unknown`. These locations conservatively exclude both their Overworld and 8:1 Nether interpretations. **Review ambiguous dimensions** lets you confirm them, up to 20 at a time. Known Nether locations are projected at 8:1 into the Overworld; End locations are saved but do not exclude Overworld sites. The review section and candidate coordinates are hidden while Coord Hider is active in either mode.
4. **Find another candidate.** Independent `SecureRandom` X/Z samples must pass the current Overworld border, spawn distance, axes/diagonals, configured square rings, round-coordinate and private-location exclusions. **known-location-clearance** is a configurable minimum in Overworld blocks; a larger radius saved on an individual location still applies. The default is 10,000 blocks. The search stops after 8,192 attempts and reports when no candidate is found. A candidate passes geometry filters only: travel is required to inspect its terrain, and that journey can leave a trail.

The report gives reasons alongside **near known coordinates**, **clues detected**, or **no clues detected in inspected chunks**, plus any coverage gaps. It counts Meteor storage-block types, active-portal sections, obsidian/crying-obsidian sections, and sections with signs, torches, rails, scaffolding or ladders. These are **section hints, not structure counts**: natural structures and retained palette entries can produce false positives. Other players, item frames, armour stands, villagers and minecarts are sampled as one combined entity count when entering a chunk and once per second while stationary.

With compatible XaeroPlus and World Map installed, the same report reads enabled XaeroPlus **OldChunks** and **PaletteNewChunks** cache flags for received chunks: old-generation, newly generated and inverse-new/previously generated. The module does not enable or change those detectors. Disabled detectors, absent/incompatible optional mods and missing flags remain unavailable or unknown. Flags are heuristic evidence, may overlap and do not identify a player, visit date or base. Upgrades are not inferred. Scouting can create its own chunk history; no colour legend or hidden terrain-history guarantee is assumed.

Private lists reload automatically when starting scouting or joining that server. They live under `meteor-client/base-scout-private/` in the active game directory, in a file keyed by a hash of the server address; singleplayer worlds use separate keys. Default ports and hostname case are normalized. Different address aliases can have separate lists, so import the same sanitized JSON if you use an alias. These files are local, excluded from Git and absent from the JAR. They still contain real locations: do not share them or include them in configuration bundles. An accepted import may finish saving its captured server's list after scouting stops; it cannot apply results to a different server's report.

Filter settings are saved normally. Candidates, nearby observations and up to 128 optional straight route segments stay in memory and clear on stop, join or disconnect; the saved private list remains. **Session route exclusions** supports Overworld and Nether endpoints in the same dimension, projected at 8:1 where appropriate. Use separate segments for bends. Candidate-filter settings are collapsed initially; ring radii start empty because known roads depend on the server. To remove entries or change an individual radius, edit the coordinate-only JSON and restart scouting. Imports merge rather than delete entries.

Empty results do not establish that terrain is unvisited or concealed. The module does not detect every tunnel, trail, player build or ESP-visible object. Inspect the approach and both dimensions yourself, including projected Nether corridors and portal offsets. Keep storage compact, test return access before removing temporary portals, separate reserves, control visitors, and audit screenshots/maps/notifications for leaks. No survival base is permanently unfindable, and scouting cannot erase an existing approach.

## Coordinate hider

Turn on **coord-hider** before recording or streaming, then choose a **mode**:

- **Hide** (default) suppresses F3 location entries and position HUD elements, and redacts recognised coordinate text.
- **Randomize** uses one synthetic XYZ location generated by Java's `SecureRandom`, without reading or offsetting your real position. The values stay fixed during the session, including while you move. Activation, a world join, changing to Randomize, or **Regenerate fake coordinates** creates a fresh location. The values and generator state are not settings and are not saved.

Both modes use the existing coverage switches:

- **hide-f3-location** protects F3 location entries. Randomize shows synthetic XYZ, block and derived chunk coordinates. Targeted block/fluid/entity, section and terrain-detail entries remain hidden.
- **hide-meteor-coordinate-hud** protects Meteor position expressions, including split axes, camera/target positions and opposite-dimension coordinates. Recognised position reads are replaced before the HUD script runs. Coordinate templates with unsupported syntax are suppressed. Regeneration or mode changes refresh the cached HUD immediately.
- **mask-coordinate-text** protects recognised triples, labelled axes and stash coordinate pairs in vanilla and Meteor text. Supported formats share the same synthetic location. Ordinary non-location text is preserved.

This is display protection, not complete location anonymity. The module does not change your player position, movement packets, route data or global Starscript values. Exports, logs, clipboard content and configured webhook messages keep real coordinates. Maps, world geometry, individual input fields and overlays using unrecognised formats can still reveal your location. Choose Hide if you prefer to suppress coordinates instead of displaying a fixed synthetic location.

## Other modules

- **InstaMine** attempts to finish mining immediately when your current tool and effects give more than `0.5` block-breaking progress per tick, matching FMod's previous threshold. It sends sequenced start/stop mining actions and predicts the local removal; the server can reject or correct the break. Slower blocks use normal mining. Creative mode, game-mode/tool restrictions, air and unbreakable blocks keep normal behaviour. No optional mods are required.
- **NoTextureRotations** stops the random rotation of some block textures, such as grass, sand and bedrock. Rejoin the world or reload graphics after you turn it on. Based on [rfresh2/NoTextureRotations](https://github.com/rfresh2/NoTextureRotations/).
- **FrameDupe** places and breaks items in nearby item frames in a fast loop. Some servers have duplication bugs that this can trigger. Range, frame order and each delay are settings.

## Commands

The movement commands are based on [JorianWoltjer/LiveOverflowMod](https://github.com/JorianWoltjer/LiveOverflowMod).

| Command | Aliases | What it does |
| --- | --- | --- |
| `.base-scout <action>` | `.bs` | Scouting, HUD, candidates and private exclusions; use `.bs help`. |
| `.stashmover <action>` | `.sm` | Courier trials, shipments, area corners, status and stop. |
| `.pearl-loader <action>` | `.pl`, `.pearlloader` | Mark and manually trigger a named operator trapdoor. |
| `.chest-tracker [search <text>]` | `.ct`, `.chestindex` | Browse/search container records; also `status` and `rescan`. |
| `.signhistory` | `.shistory` | Browse observed sign history. |
| `.roadtrip <action>` | `.rt` | Travel monitoring and current-dimension targets. |
| `.teleport <x> <y> <z> [ticks]` | `.tp` | Teleports you a short distance. `ticks` sets how many movement packets are sent, which can change how the server handles the move. |
| `.autoclip <direction>` | `.ac`, `.aclip` | Moves you up (`1`) or down (`-1`) through blocks to the nearest air space. |
| `.shclip <distance>` | `.hc`, `.shc` | Moves you horizontally through walls in the direction you are looking. |
| `.dclip <distance>` | `.dc` | Moves you in the exact direction you are looking, including up, down or diagonally. |
| `.clubmate clubmate` | `.cm`, `.clubm`, `.cmate` | Uses a specific chest on the LiveOverflow server to get a "Club-Mate". |
