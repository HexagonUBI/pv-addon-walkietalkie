<div align=center>

![NeoForge](https://img.shields.io/badge/NeoForge-21.1.233-d8b62b?style=plastic&logo=curseforge&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brown?style=plastic&logo=minecraft&logoColor=white)
![Requires Plasmo Voice](https://img.shields.io/badge/Requires-Plasmo%20Voice-5865F2?style=plastic)
[![Github Downloads](https://img.shields.io/github/downloads/SimpleFoxOfficial/pv-addon-walkietalkie/total?style=plastic&logo=github&label=GitHub&color=purple)](https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie/releases)
[![Latest Release](https://img.shields.io/github/v/release/SimpleFoxOfficial/pv-addon-walkietalkie?style=plastic&logo=github&color=purple)](https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie/releases)
[![Modrinth](https://img.shields.io/modrinth/dt/KbXmELYr?style=plastic&logo=modrinth&logoColor=white&label=Modrinth&color=green
)](https://modrinth.com/mod/pv-addon-walkietalkie)
<img alt="banner01" src="https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie/blob/main/.github/ImagesStuff/walkietalkie_banner2-1.png?raw=true">

</div>

---

### Minecraft Version: 1.21.1

### NeoForge Version: 21.1.233

### Dependency: [Plasmo Voice](https://modrinth.com/mod/1bZhdhsH)

---

<img alt="banner01" src="https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie/blob/main/.github/ImagesStuff/walkietalkie_banner3-1.png?raw=true">
<img alt="banner01" src="https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie/blob/main/.github/ImagesStuff/walkietalkie_banner3-2.png?raw=true">

<br>

<img alt="banner01" src="https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie/blob/main/.github/ImagesStuff/walkietalkie_banner4-1.png?raw=true">
<img alt="banner01" src="https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie/blob/main/.github/ImagesStuff/walkietalkie_banner4-2.png?raw=true">

### Walkie-Talkie

- RMB Tap: Toggle the walkie-talkie
- RMB Held: Speak in the walkie-talkie
- Shift+RMB: Configure frequency and toggle walkie-talkie (is a menu)

### Radio Station

A placeable block with its own frequency dial and two module slots. Place it on any floor.

- **Microphone Module** - lets the station pick up players speaking near it and transmit them onto its frequency
- **Speaker Module** - lets the station play everything on its frequency out loud to anyone nearby

Each module has its own toggle switch in the station's menu, so you can run a station as a
receiver, a transmitter, or both. Pickup range is configurable and defaults to 5 blocks.

# Configuration

Settings live in `config/walkietalkie.toml` and can also be edited in-game from
**Mods -> pv-addon-walkietalkie -> Config**, without restarting or editing files by hand.

| Setting | Default | Description |
|---|---|---|
| `frequency.min-frequency` | `80.0` | Lowest tunable frequency |
| `frequency.max-frequency` | `999.9` | Highest tunable frequency |
| `station.mic-range` | `5.0` | How close you must be for a station's microphone to pick you up |
| `station.show-voice-icon` | `true` | Show the Plasmo Voice speaker icon above an active station |
| `station.radio-effect` | `false` | Apply the radio effect server-side instead of on the listener's client |

Client options (radio voice effect, SFX volume, default activation type) are under
**Plasmo Voice -> Addons -> Walkie Talkie**.

# How To Install

1. Install [Plasmo Voice](https://modrinth.com/mod/1bZhdhsH) on both client and server - this addon does nothing without it
2. Download the latest jar from [Releases](https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie/releases)
3. Drop it in your `mods` folder (client + server)
4. Launch the game, craft a Walkie-Talkie, and tune in

# Optional Integrations

Both are detected automatically at runtime. Neither is required, and nothing breaks if they are absent.

- **[pv-voice-changer](https://github.com/imsawiq/pv-voice-changer)** - if installed, its Radio preset is used
  for the radio effect. Without it, a built-in equivalent is used instead.
- **[Sable](https://github.com/ryanhcode/sable)** - if installed, Radio Stations placed on sub-levels
  (vehicles, ships, contraptions) are positioned at their real world location, so audio and the
  station menu behave normally while they move.

# Building from Source

```bash
git clone https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie.git
cd pv-addon-walkietalkie
./gradlew build
```

The built jar will be in `build/libs/`.

# License

Source-available, all rights reserved. You may read, study and contribute to the code, and use the
compiled mod (including in free modpacks) with credit. Redistribution, reuse of the code or assets
in other projects, commercial use and re-uploads require prior written permission. See [LICENSE](LICENSE)
for the full terms.
<br><br>
<img alt="banner01" src="https://github.com/SimpleFoxOfficial/pv-addon-walkietalkie/blob/main/.github/ImagesStuff/walkietalkie_banner5-1.png?raw=true">

Developed by [SimpleFoxOfficial](https://github.com/SimpleFoxOfficial) and **MrEri** for the [Vector Point](https://github.com/SimpleFoxOfficial/VectorPoint) modpack, built on top of [Plasmo Voice](https://github.com/plasmoapp/plasmo-voice) by Apehum.
