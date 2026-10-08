<div align="center">

<img src="assets/mixify-icon.png" alt="Mixify app icon" width="160" />

# Mixify

**A modern, open-source music player for Android.**

Synced lyrics · Offline listening · Personalized Mixes · Listen Together

<br/>

[![Latest Release](https://img.shields.io/github/v/release/scisims12/Mixify?style=for-the-badge&labelColor=0d1117)](https://github.com/scisims12/Mixify/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/scisims12/Mixify/total?style=for-the-badge&labelColor=0d1117)](https://github.com/scisims12/Mixify/releases)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge&labelColor=0d1117)](LICENSE)
[![Telegram](https://img.shields.io/badge/Telegram-Join-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white&labelColor=0d1117)](https://t.me/mixify20)

<br/>

[Download](#download) · [Screenshots](#screenshots) · [Features](#features) · [Installation](#installation) · [FAQ](#faq) · [Credits](#credits-and-origins)

</div>

---

## About

Mixify brings playback, synced lyrics, offline listening, playlists, search and shared listening together in one clean interface.

It is built on top of the open-source [InnerTune](https://github.com/z-huang/InnerTune) and [OuterTune](https://github.com/DD3Boh/OuterTune) projects. See [Credits and Origins](#credits-and-origins) for what Mixify changed and who to thank.

## Screenshots

<div align="center">

<img src="assets/screenshots/home.png" alt="Mixify Home" width="30%" />
<img src="assets/screenshots/search.png" alt="Mixify Search" width="30%" />
<img src="assets/screenshots/player.png" alt="Mixify Player" width="30%" />

<img src="assets/screenshots/lyrics.png" alt="Mixify Lyrics" width="30%" />
<img src="assets/screenshots/queue.png" alt="Mixify Queue" width="30%" />
<img src="assets/screenshots/mix.png" alt="Mixify Mix" width="30%" />

</div>

## Features

| | Feature | What you get |
|---|---|---|
| 🎧 | **Playback** | Background playback, queue management, and a redesigned mini player and Now Playing screen |
| 🎤 | **Synced lyrics** | Time-synced lyrics on a dedicated screen, one tap away from the player |
| 🎵 | **Personalized Mix** | Mixes and collections built around your listening data and preferences |
| 👥 | **Listen Together** | Shared rooms with synchronized playback, running on a custom Mixify server |
| 📥 | **Offline listening** | Keep supported songs saved and listen without a connection |
| 📚 | **Library** | Playlists, favourites, recently played and quick access to saved music |
| 🔍 | **Search and discovery** | Search songs and playlists, and browse for something new |
| 🎨 | **Design** | Dark interface with a Liquid Glass bottom navigation and an Apple-inspired player |

## Download

<div align="center">

<a href="https://github.com/scisims12/Mixify/releases/latest">
  <img src="https://img.shields.io/badge/GET%20IT%20ON-GitHub-FFFFFF?style=for-the-badge&logo=github&logoColor=white&labelColor=0d1117" alt="Get Mixify on GitHub" height="55">
</a>
<a href="https://apkpure.com/p/com.mixify.app">
  <img src="https://img.shields.io/badge/GET%20IT%20ON-APKPure-24CD77?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117" alt="Get Mixify on APKPure" height="55">
</a>

</div>

**Requires:** Android **TODO: minimum version** or newer.

> Only download Mixify from the official GitHub releases page or the APKPure page linked above.

## Installation

1. Download the latest APK from [GitHub Releases](https://github.com/scisims12/Mixify/releases/latest) or [APKPure](https://apkpure.com/p/com.mixify.app).
2. Open the APK. If Android asks, allow installs from that source.
3. Tap **Install**, then open Mixify and start listening.

## FAQ

**Does Mixify support offline playback?**
Yes, for supported songs you have saved.

**Does Mixify have lyrics?**
Yes. Mixify has a synced lyrics view you can open from the player.

**Can I listen with friends?**
Yes. Listen Together lets you listen in shared rooms with synchronized playback.

**Where does Mixify get its music?**
TODO: state the source plainly (which service or API Mixify streams from).

**Do I need an account to use Mixify?**
TODO: yes or no, and what it is used for.

**Where can I report a problem?**
See [Community and Support](#community-and-support).

## Community and Support

- 💬 **Updates and discussion:** [Join the Telegram community](https://t.me/mixify20)
- 🐛 **Found a bug?** [Open an issue](https://github.com/scisims12/Mixify/issues) and include your Mixify version, Android version, device model, steps to reproduce, and a screenshot or screen recording if possible.
- 💡 **Have an idea?** [Open an issue](https://github.com/scisims12/Mixify/issues) describing the feature you would like.
- 🔒 **Privacy:** [Read the Mixify Privacy Policy](https://scisims12.github.io/Mixify/privacy-policy.html)

## Contributing

Contributions are welcome. Fork the repository, create a branch, make and test your changes, then open a Pull Request.

Please never commit secrets such as `local.properties`, `key.properties`, `.jks` / `.keystore` files, API keys, access tokens or service account files. If a secret is ever published by accident, revoke or rotate it immediately.

<details>
<summary><b>Build from source</b></summary>

<br/>

**Requirements**

- Android Studio (latest stable)
- JDK TODO: version
- Android SDK TODO: compile SDK version

**Steps**

```bash
git clone https://github.com/scisims12/Mixify.git
cd Mixify
```

Open the project in Android Studio, let Gradle sync, then run it on a device or emulator. To build a debug APK from the command line:

```bash
./gradlew assembleDebug
```

</details>

## Credits and Origins

Mixify is built on top of the open-source **InnerTune** and **OuterTune** projects and is distributed under the **GNU GPL-3.0**. Reused source code, libraries and assets keep their original licenses and attribution. For the exact code lineage, see the repository's source files and commit history.

### What Mixify adds

| Area | Changes |
|---|---|
| 🎨 **Interface** | Liquid Glass bottom navigation, reworked mini player, Apple-inspired Now Playing screen, and customized lyrics, home, search and library screens |
| 🎵 **Mix** | A dedicated Mix section built around the user's listening data and music preferences |
| 👥 **Listen Together** | Shared listening rooms with playback sync, backed by a custom server |
| ⚡ **Playback** | Work on music fetching, playback integration, interface behavior and bug fixes |

> This section describes Mixify's own modifications. It does not claim that every component was written from scratch or that all upstream code originated with Mixify.

### Upstream projects

| Project | Authors |
|---|---|
| [**InnerTune**](https://github.com/z-huang/InnerTune) | [Zion Huang](https://github.com/z-huang) · [Malopieds](https://github.com/Malopieds) |
| [**OuterTune**](https://github.com/DD3Boh/OuterTune) | [Davide Garberi](https://github.com/DD3Boh) · [Michael Zh](https://github.com/mikooomich) |

InnerTune itself credits [ViMusic](https://github.com/vfsfitvnm/ViMusic) by vfsfitvnm as an inspiration, so thanks to that project as well.

### Libraries and integrations

| Project | Contribution |
|---|---|
| [**Better Lyrics**](https://better-lyrics.boidu.dev) | Time-synced lyrics with word-by-word highlighting and YouTube Music integration |

Thank you to the wider open-source community for every library, tool and API that powers this project.

## License

Mixify is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for the full text.

You are free to use, study, modify and share Mixify under the terms of the GPL-3.0. Modified or redistributed versions must be released under the same license, with their source code made available. Third-party libraries, services and content used by Mixify remain under their own licenses.

## Disclaimer

Mixify is an independent project. It is not affiliated with, endorsed by, or maintained by InnerTune, OuterTune, their developers, or any third-party music or lyrics service it connects to.

Trademarks, service names, music, artwork, APIs and other third-party content remain the property of their respective owners, and Mixify does not claim ownership of them. Users and contributors are responsible for following the applicable licenses, platform terms and laws when using third-party services or content.

---

<div align="center">

**Mixify · Listen. Discover. Enjoy.**

Made with ❤️ for music lovers by [Priyanshu Sharma](https://www.instagram.com/sharmaajikabadabeta)

[![GitHub](https://img.shields.io/badge/GitHub-scisims12-181717?style=for-the-badge&logo=github)](https://github.com/scisims12)
[![Star Mixify](https://img.shields.io/github/stars/scisims12/Mixify?style=for-the-badge&logo=github&label=Star%20Mixify&labelColor=0d1117)](https://github.com/scisims12/Mixify)

</div>
