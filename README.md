# FMod

<div align="center">
  <img src="src/main/resources/assets/template/icon.PNG" alt="FMod Logo" width="200"/>
</div>

**FMod** is an addon for the [Meteor Client](https://meteorclient.com/), a modern Minecraft utility mod. FMod enhances your gameplay with stash hunting, route planning, flight helpers, coordinate hiding, block rendering controls, and item frame automation, with some specifically tailored for servers like the LiveOverflow server.

<!-- Fancy badges -->
<div align="center">
  <a href="https://github.com/fluffysnaff/fmod/releases"><img src="https://img.shields.io/badge/Version-0.2.0-blue" alt="Version"></a>
  <img src="https://img.shields.io/badge/Minecraft-26.3-green" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Meteor%20Client-Addon-purple" alt="Meteor Addon">
  <img src="https://img.shields.io/badge/spaghetti%20code-yes-success?logo=java" alt="Spagetti code: yes">
</div>

---

## 🌟 Features

FMod introduces a variety of modules, commands, and HUD elements designed to give you an edge:

### ✨ Modules

*   **`NoTextureRotations`**: Disables the random rotation of certain block textures (e.g., grass, sand, bedrock), providing a more consistent and potentially cleaner visual experience. *Requires a world rejoin or graphics reload to take full effect.*
*   **`FrameDupe`**: A utility that automatically interacts with nearby item frames to duplicate items. It works by quickly placing and breaking items in frames, which can trigger duplication glitches on certain servers.

### Stashhunting port

Ported from the `26.1.2` branch of [miles352/meteor-stashhunting-addon](https://github.com/miles352/meteor-stashhunting-addon). All modules are in the **FMod** category.

| Modules | Features |
| --- | --- |
| BetterStashFinder, OldChunkNotifier | Loaded-chunk storage detection, old-chunk filters, stash records and CSV/JSON export, map markers, optional webhook alerts. |
| TrailFollower, TrailMaker, Search Area, GotoPosition | Follow old-chunk trails, record routes from XaeroPlus drawing highlights, resume rectangle or spiral searches, steer toward a destination. |
| ElytraFlyPlusPlus, Pitch40Util, AFKVanillaFly | Highway obstacle passing with Baritone, Pitch40 height bounds and fireworks, level-flight assistance. |
| AutoLogPlus, DiscordNotifs | Configurable logout triggers and opt-in webhook notifications. Webhook URLs are empty by default. |
| ChestIndex, HighlightOldLava, VanityESP | Container inventory totals and JSON export, legacy lava highlights, map-art and banner highlights. |
| AutoPortal, AutoEXPPlus, GrimAirPlace, NoJumpDelay | Portal building, equipment repair, air placement, and removal of the player's jump cooldown. |

Weather and EntityList HUD elements are available in the **FMod** HUD group.

Map modules appear when their optional dependencies are installed. The port was startup-tested with:

| Optional dependency | Minecraft 26.3 version |
| --- | --- |
| Xaero's World Map | `fabric-26.3-1.47.2` |
| Xaero's Minimap | `fabric-26.3-26.6.2` |
| XaeroPlus | `2.37.2+fabric-26.3` |
| Meteor Baritone | `26.3-SNAPSHOT` |
| Fabric API (required by Xaero's maps) | `0.164.0+26.3` |

BetterStashFinder, OldChunkNotifier, and TrailMaker require both maps and XaeroPlus. TrailFollower requires Xaero's World Map, XaeroPlus, and Baritone. ElytraFlyPlusPlus requires Baritone. FMod also loads without these optional mods.

### Coordinate hider

Enable **coord-hider** before recording or streaming:

* **hide-f3-location** hides F3 position, chunk/section, terrain-height and targeted block/fluid/entity entries.
* **hide-meteor-coordinate-hud** suppresses Meteor text HUD elements using position Starscript expressions, including camera, target, individual axes and opposite-dimension coordinates.
* **mask-coordinate-text** redacts numeric coordinate triples, labelled axes, and stash-coordinate pairs in vanilla and Meteor text.

This changes displayed text only. Actual positions, packets, saved routes, stash exports, logs, clipboard content and configured webhook notifications retain their original coordinates. Maps and world geometry remain visible. Arbitrary custom text or third-party overlays can use formats the text masker does not recognise.

### ⌨️ Commands

Commands are prefixed with a `.` (dot), Meteor Client's default command prefix.

*   **`.teleport` (aliases: `.tp`)**
    *   Usage: `.tp <x> <y> <z> [ticks]`
    *   Teleports you short distances. The optional `ticks` argument specifies the number of movement packets sent, which can influence how the teleport is processed by the server.
*   **`.autoclip` (aliases: `.ac`, `.aclip`)**
    *   Usage: `.autoclip <direction>`
    *   Clips you through the ground either up (`1`) or down (`-1`) to the nearest air block.
*   **`.shclip` (aliases: `.hc`, `.shc`)**
    *   Usage: `.shclip <distance>`
    *   Clips you horizontally through walls in the direction you are looking.
*   **`.dclip` (aliases: `.dc`)**
    *   Usage: `.dclip <distance>`
    *   Clips you in your direct view direction (can be horizontal, vertical, or diagonal).
*   **`.clubmate` (aliases: `.cm`, `.clubm`, `.cmate`)**
    *   Usage: `.clubmate clubmate`
    *   A specialized command designed for the LiveOverflow server to interact with a specific chest and obtain a "Club-Mate."

---

## 🚀 How to Use

1.  **Install Meteor Client**: If you haven't already, download and install [Meteor Client](https://meteorclient.com/download) for Minecraft 26.3. Use Java 25 or newer and Fabric Loader 0.19.5 or newer. Meteor bundles the Fabric API components it needs.
2.  **Download FMod**:
    *   It's only possible now to build it yourself from the source code(see below).
3.  **Install FMod**: Place the downloaded `fmod-[version].jar` file into your `.minecraft/mods/` folder. This folder should also contain your Meteor Client 26.3 jar.
4.  **Launch Minecraft**: Start Minecraft using the Fabric loader profile from your Minecraft launcher. FMod will load automatically as an addon to Meteor Client.

---

## 🛠️ Building from Source (Optional)

Building requires a JDK 25 installation. Set `JAVA_HOME` to that JDK, then use the included Gradle 9.8.1 wrapper. The project targets Minecraft 26.3 and Meteor Client `26.3-SNAPSHOT` using Mojang names (no Yarn mappings).

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/fluffysnaff/fmod.git
    cd fmod
    ```
2.  **Build the Project**:
    Use Gradle Wrapper to build the project.
    *   On Windows:
        ```bash
        .\gradlew.bat build
        ```
    *   On macOS/Linux:
        ```bash
        bash ./gradlew build
        ```
3.  **Locate the Jar**: The compiled `.jar` file (e.g., `fmod-0.2.0.jar`) will be located in the `build/libs/` directory. You can then use this jar as described in the "How to Use" section.

---

## License, source and credits

FMod's original code and modifications are licensed under the [GNU General Public License version 3](LICENSE) (`GPL-3.0-only`).

Meteor-derived code retains its copyright notices and GPL licensing. The stashhunting port preserves upstream attribution; its upstream revision has no license file. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for provenance and the outstanding permission requirement for that imported code.

Credits:

*   **[miles352 and stashhunting contributors](https://github.com/miles352/meteor-stashhunting-addon/)**
*   **[Meteor Client Development Team](https://github.com/MeteorDevelopment/meteor-client/)**
*   **[rfresh2/NoTextureRotations](https://github.com/rfresh2/NoTextureRotations/)**
*   **[JorianWoltjer/LiveOverflowMod](https://github.com/JorianWoltjer/LiveOverflowMod)**

---

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=fluffysnaff/fmod&type=Date)](https://www.star-history.com/#fluffysnaff/fmod&Date)
