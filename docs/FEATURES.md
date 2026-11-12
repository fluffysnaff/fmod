# FMod features

All FMod modules are in the **FMod** category of Meteor's module menu. The Weather and EntityList HUD elements are in the **FMod** HUD group. Commands use Meteor's command prefix, which is `.` by default.

Back to the [README](../README.md).

## Stash hunting, travel and alerts

These modules are ported from the `26.1.2` branch of [miles352/meteor-stashhunting-addon](https://github.com/miles352/meteor-stashhunting-addon). See [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) for their source and authors.

| Modules | What they do |
| --- | --- |
| BetterStashFinder, OldChunkNotifier | Find storage blocks in loaded chunks, filter by old chunks, save stash records as CSV and JSON files, add map markers, and send optional webhook alerts. |
| TrailFollower, TrailMaker, SearchArea, GotoPosition | Follow old-chunk trails, record routes from XaeroPlus drawing highlights, resume rectangle or spiral searches, and steer toward a destination. |
| ElytraFlyPlusPlus, Pitch40Util, AFKVanillaFly | Pass highway obstacles with Baritone, keep Pitch40 height bounds and fireworks, and hold level flight. |
| AutoLogPlus, DiscordNotifs | Log out on more triggers, and send notifications to your own Discord webhook. Webhook URLs are empty by default. |
| ChestIndex, HighlightOldLava, VanityESP | Show the total count of items in your chests and export it as JSON, highlight old flowed lava, and highlight map art and banners. ChestIndex can break with large numbers of chests. |
| AutoPortal, AutoEXPPlus, GrimAirPlace, NoJumpDelay | Build portals, repair equipment with experience, place blocks in the air, and remove the jump cooldown. |

## Optional mods

Some modules need other mods. FMod still loads without them, and the modules that need them do not appear.

| Module | Needs |
| --- | --- |
| BetterStashFinder, OldChunkNotifier, TrailMaker | Xaero's World Map, Xaero's Minimap, and XaeroPlus |
| TrailFollower | Xaero's World Map, XaeroPlus, and Baritone |
| ElytraFlyPlusPlus | Baritone |

The port was tested at startup with these versions for Minecraft 26.3:

| Optional mod | Version |
| --- | --- |
| Xaero's World Map | `fabric-26.3-1.47.2` |
| Xaero's Minimap | `fabric-26.3-26.6.2` |
| XaeroPlus | `2.37.2+fabric-26.3` |
| Meteor Baritone | `26.3-SNAPSHOT` |
| Fabric API (required by Xaero's maps) | `0.164.0+26.3` |

## Coordinate hider

Turn on **coord-hider** before you record or stream. It has three settings:

- **hide-f3-location** hides the F3 position, chunk and section, terrain height, and targeted block, fluid and entity lines.
- **hide-meteor-coordinate-hud** hides Meteor text HUD elements that use position Starscript values. This includes camera, target, single-axis and opposite-dimension coordinates.
- **mask-coordinate-text** hides number triples that look like coordinates, labelled axes, and stash coordinate pairs in vanilla and Meteor text.

The module changes only the text on your screen. Your real position, network packets, saved routes, stash exports, logs, clipboard content and webhook messages keep the real coordinates. Maps and world geometry stay visible. Custom text and overlays from other mods can use formats that the text masker does not detect.

## Other modules

- **NoTextureRotations** stops the random rotation of some block textures, such as grass, sand and bedrock. Rejoin the world or reload graphics after you turn it on. Based on [rfresh2/NoTextureRotations](https://github.com/rfresh2/NoTextureRotations/).
- **FrameDupe** places and breaks items in nearby item frames in a fast loop. Some servers have duplication bugs that this can trigger. Range, frame order and each delay are settings.

## Commands

The movement commands are based on [JorianWoltjer/LiveOverflowMod](https://github.com/JorianWoltjer/LiveOverflowMod).

| Command | Aliases | What it does |
| --- | --- | --- |
| `.teleport <x> <y> <z> [ticks]` | `.tp` | Teleports you a short distance. `ticks` sets how many movement packets are sent, which can change how the server handles the move. |
| `.autoclip <direction>` | `.ac`, `.aclip` | Moves you up (`1`) or down (`-1`) through blocks to the nearest air space. |
| `.shclip <distance>` | `.hc`, `.shc` | Moves you horizontally through walls in the direction you are looking. |
| `.dclip <distance>` | `.dc` | Moves you in the exact direction you are looking, including up, down or diagonally. |
| `.clubmate clubmate` | `.cm`, `.clubm`, `.cmate` | Uses a specific chest on the LiveOverflow server to get a "Club-Mate". |
