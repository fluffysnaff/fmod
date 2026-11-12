# FMod

<div align="center">
  <img src="src/main/resources/assets/template/icon.PNG" alt="FMod Logo" width="200"/>
</div>

**FMod** is an addon for the [Meteor Client](https://meteorclient.com/), a modern Minecraft utility mod. FMod enhances your gameplay with block rendering controls, item frame automation, and movement commands, with some specifically tailored for servers like the LiveOverflow server.

<!-- Fancy badges -->
<div align="center">
  <a href="https://gitlab.com/fluffysnaff/fmod/-/releases"><img src="https://img.shields.io/badge/Version-0.1.2-blue" alt="Version"></a>
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
    git clone https://gitlab.com/fluffysnaff/fmod.git
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
3.  **Locate the Jar**: The compiled `.jar` file (e.g., `fmod-0.1.2.jar`) will be located in the `build/libs/` directory. You can then use this jar as described in the "How to Use" section.

---

## 🙏 Credits

This project wouldn't be possible without the work and inspiration from others:

*   **[Meteor Client Development Team](https://github.com/MeteorDevelopment/meteor-client/)**
*   **[rfresh2/NoTextureRotations](https://github.com/rfresh2/NoTextureRotations/)**
*   **[JorianWoltjer/LiveOverflowMod](https://github.com/JorianWoltjer/LiveOverflowMod)**

---
