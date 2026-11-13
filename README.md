<div align="center">
  <img src="docs/assets/readme/logo.png" alt="FMod logo, a red pixel-art letter F with a small purple plus sign" width="128">

  <h1>FMod</h1>

  <p>Stash hunting, travel and streaming tools for Meteor Client on Minecraft 26.3.</p>

  <p>
    <a href="https://github.com/fluffysnaff/fmod/releases/tag/v0.2.0"><b>Download v0.2.0 (prerelease)</b></a>
    &nbsp;|&nbsp;
    <a href="docs/FEATURES.md">All features</a>
    &nbsp;|&nbsp;
    <a href="#build-from-source">Build from source</a>
  </p>
</div>

FMod is an addon for [Meteor Client](https://meteorclient.com/), a Minecraft client mod. All of its modules are in the **FMod** category of Meteor's menu.

This branch contains the upcoming **0.3.0** changes. Build from source to use them; the v0.2.0 prerelease has the previous module set and Hide-only coordinate protection.

## Features

<table>
<tr>
<td valign="top">

- **Find stashes.** Search Area moves you through a rectangle or a spiral. Better Stash Finder saves every chest it finds to CSV and JSON files.
- **Follow trails.** Old Chunk Notifier tells you when you reach chunks from an older Minecraft version, where players were long ago. TrailFollower and Trail Maker follow a trail for you.
- **Use Meteor flight while following trails.** TrailFollower can enable Meteor's built-in ElytraFly with your existing settings, and restores only the activation state it owns.
- **Fly with a controlled speed ramp.** VolytraFly brings Volizray's 6b6t flight module, navigation, avoidance and HUDs into FMod. Its default requested ceiling approaches 300 blocks/s; live server acceptance must be tested.
- **Scout a new base location.** BaseScout continuously checks received chunks, reads optional XaeroPlus history, and avoids private per-server exclusions. Follow its live HUD and local chat reports; use `.bs start`, `.bs hud` and `.bs next` without opening a menu.
- **Move supplies.** StashMover loads filled shulkers from marked containers, travels through manual stasis recalls or Tpa, verifies unloading, then returns empty by `/kill` and bed respawn. Empty route trials are required first; PearlLoader provides local controls for the destination operator.
- **Find stored items.** ChestTracker remembers opened containers, ChestIndex checks reachable storage, ShulkerOverview previews box contents, and DubCounter counts loaded storage and capacity.
- **Keep records and prepare supplies.** SignHistorian records observed sign changes, SignatureSign fills sign templates, StashBrander renames items through an anvil, and RoadTrip monitors travel progress and optional logout conditions.
- **Conceal coordinate displays.** Coord Hider can hide coordinates or show securely generated synthetic coordinates in F3, the Meteor HUD and recognised text. Actual movement and saved data stay unchanged.
- **Get alerts.** Auto Log Plus logs you out in more situations. Discord Notifs sends messages to your own Discord webhook.
- **Move with commands.** `.tp`, `.autoclip`, `.shclip` and `.dclip` teleport you short distances or through blocks.

[All features and settings](docs/FEATURES.md)

</td>
<td valign="top" width="193">
  <img src="docs/assets/readme/modules.png" width="193" alt="The current FMod module menu, including Base Scout, Stash Mover and Volytra Fly">
  <p><small>0.3.0 development menu with optional mods installed.</small></p>
</td>
</tr>
</table>

## Install

1. Install [Meteor Client](https://meteorclient.com/download) for Minecraft 26.3. You need Fabric Loader 0.19.5 or newer and Java 25 or newer.
2. Download `fmod-<version>.jar` from the [v0.2.0 prerelease](https://github.com/fluffysnaff/fmod/releases/tag/v0.2.0) and put it in your `.minecraft/mods` folder.
3. Start Minecraft with your Fabric profile. Press Right Shift to open Meteor's menu.

The map and trail modules need Xaero's World Map, Xaero's Minimap and XaeroPlus. TrailFollower also needs Baritone. Without these mods, the modules that need them do not appear. See the [tested versions](docs/FEATURES.md#optional-mods).

Many servers do not allow client mods. Read a server's rules before you use FMod on it.

## Build from source

With JDK 25 set as `JAVA_HOME`:

```bash
git clone https://github.com/fluffysnaff/fmod.git
cd fmod
bash ./gradlew build
```

On Windows, use `.\gradlew.bat build`. The jar is written to `build/libs/`.

## License and credits

FMod is licensed under the [GNU General Public License version 3](LICENSE). It builds on work by [miles352 and the stashhunting contributors](https://github.com/miles352/meteor-stashhunting-addon/), the [Meteor Client team](https://github.com/MeteorDevelopment/meteor-client/), [rfresh2/NoTextureRotations](https://github.com/rfresh2/NoTextureRotations/) and [JorianWoltjer/LiveOverflowMod](https://github.com/JorianWoltjer/LiveOverflowMod). [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) explains the source and license status of the imported code.
