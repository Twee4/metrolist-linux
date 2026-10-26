# Metrolist Linux

A native Linux desktop project for the Metrolist ecosystem, built as a separate codebase from the Android app at `https://github.com/Untet01/Metrolist`.

This repo intentionally keeps the Android project as-is and moves the Linux implementation forward in a desktop-first architecture.

## Architecture

- `shared/` holds the domain and data logic that can be reused across platforms
- `app/` is the desktop UI using Compose for Desktop
- the Android repo remains the source of truth for mobile-specific behavior

## Current status

This project now includes:

- a shared Kotlin module for domain logic
- a Compose desktop shell
- navigation rail and starter screens
- a minimal search and library scaffold
- app structure ready for incremental feature porting

## Run

```bash
./gradlew :app:run
```

If you do not have the Gradle wrapper installed locally yet, install Gradle and then run the same command.

## Near-term roadmap

1. Port search and metadata logic from the Android app into `shared`
2. Add a real library screen and queue management
3. Add a player panel for playback controls
4. Introduce Linux-specific system integration (tray, notifications)
5. Package the app for Linux distributions

## Repo structure

```text
metrolist-linux/
├── app/
│   └── src/desktopMain/kotlin/com/metrolist/linux/Main.kt
├── shared/
│   └── src/commonMain/kotlin/com/metrolist/shared/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── LICENSE
├── README.md
└── .gitignore
```
