# Metrolist Linux

A native Linux desktop port of the Metrolist ecosystem.

This project is intentionally independent of the Android at `https://github.com/MetrolistGroup/Metrolist` and focuses on building a desktop-first version using Kotlin Multiplatform and Compose for Desktop.

## Status

The project now includes:

- a shared Kotlin state layer
- a Compose desktop shell
- a search screen prototype
- a library/settings foundation
- a clear path for incremental feature porting from the Android app

## Run

Requires JDK 17+. If `gradlew` is missing, generate it once with a local Gradle install:

```bash
gradle wrapper --gradle-version 8.14.3
```

Then:

```bash
./gradlew :app:run
```

### Audio playback

Playback uses [`mpv`](https://mpv.io) with [`yt-dlp`](https://github.com/yt-dlp/yt-dlp) to stream audio, so both
must be installed and on your `PATH`:

```bash
sudo apt install mpv yt-dlp      # Debian/Ubuntu (distro yt-dlp can lag; `pipx install yt-dlp` stays current)
sudo dnf install mpv yt-dlp      # Fedora
sudo pacman -S mpv yt-dlp        # Arch
```

YouTube changes often. If tracks stop playing, update `yt-dlp` first.

### Media keys and "now playing" (MPRIS)

While the app runs it registers on the D-Bus session bus as an [MPRIS](https://specifications.freedesktop.org/mpris-spec/latest/)
player, so media keys, GNOME/KDE "now playing" widgets and `playerctl` can control it. To check it works:

```bash
playerctl -l                      # should list "metrolist"
playerctl -p metrolist metadata   # title, artist, album, length of the current track
playerctl -p metrolist play-pause
playerctl -p metrolist next
```

If there is no D-Bus session (for example over plain SSH) the app prints one line to stderr and runs without it.

Linux packages (`.deb`, `.rpm`, AppImage) are built with `./gradlew :app:packageDistributionForCurrentOS`.

## Architecture

```text
metrolist-linux/
├── app/
│   └── src/desktopMain/kotlin/com/metrolist/linux/
├── shared/
│   └── src/commonMain/kotlin/com/metrolist/shared/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── .gitignore
├── README.md
└── LICENSE
```

## Roadmap

1. Port the core YouTube/Music API and metadata logic from the Android app
2. Add a real player panel and queue management
3. Add library browsing and playlist features
4. Add Linux-native system integration and packaging
5. Prepare a production-ready desktop build for Linux distributions
