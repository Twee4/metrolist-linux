# Metrolist Linux

A native Linux desktop port of the Metrolist ecosystem.

This project is intentionally independent of the Android at `https://github.com/MetrolistGroup/Metrolist` and focuses on building a desktop-first version using Kotlin Multiplatform and Compose for Desktop.

## Status

The project now includes:

- a shared Kotlin state layer
- a Compose desktop shell
- real YouTube Music search and audio playback
- sign-in and your library (playlists and liked songs)
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

### Signing in and your library

Search works without an account. To see your own playlists and liked songs, sign in under **Settings** by
pasting the `Cookie` header your browser sends to YouTube Music:

1. Open <https://music.youtube.com> in your browser while signed in.
2. Press F12, open the **Network** tab and reload the page.
3. Click a request to `music.youtube.com` (for example `browse`) and copy the value of the **Cookie** request header.
4. Paste it into Settings and press **Sign in**.

**The cookie works like a password for your Google account.** It is stored in
`~/.config/metrolist-linux/cookie` (or under `$XDG_CONFIG_HOME`), readable only by you, and is sent only to
`music.youtube.com`. Signing out deletes the file. If YouTube later rejects it, the Library tab says so and you
paste a fresh one. Signing in uses an unofficial interface, so use it at your own risk.

### System tray

Closing the window hides it to the tray and the music keeps playing. Click the tray icon to bring the window back,
or right-click it for play/pause, next, previous, show/hide and quit.

Not every desktop offers a tray this app can use (some Wayland setups, such as niri, do not). Without a tray,
closing the window quits the app so it can never be left running with no way to reach it. Ctrl+Q always quits.

## Packaging

Build installers yourself (output goes under `app/build/compose/binaries/main/`):

```bash
./gradlew :app:packageRpm          # .rpm for Fedora/openSUSE; needs: sudo dnf install rpm-build
./gradlew :app:packageDeb          # .deb for Debian/Ubuntu;    needs: sudo apt install fakeroot
./gradlew :app:createDistributable # portable folder (run bin/metrolist-linux), no installer needed
```

`mpv` and `yt-dlp` are not bundled, so install them separately (see "Audio playback" above).

Pushing a tag such as `v1.0.0` runs the **Release** workflow, which builds the `.deb`, `.rpm` and a portable
`.tar.gz` for both x86_64 and aarch64 and attaches them to a GitHub release. It can also be started by hand
from the Actions tab to get the files as downloadable artifacts without making a release.

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
3. Library: home recommendations, albums, artists and history (playlists and liked songs are done)
4. Add Linux-native system integration and packaging
5. Prepare a production-ready desktop build for Linux distributions
