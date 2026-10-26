# Metrolist Linux

A native Linux desktop version of Metrolist built with Kotlin Multiplatform and Compose for Desktop.

This repository is intentionally separated from the Android project at `https://github.com/Untet01/Metrolist`.
The Android app remains the primary mobile implementation; this repo is the Linux-native port foundation.

## Goals

- Reuse core music API and domain logic from the Android project
- Build a Linux desktop app with Kotlin Multiplatform
- Create a clean shared-layer architecture for future porting
- Keep the Android project untouched while developing the desktop version

## Current status

This is the starter project scaffold. It includes:

- a shared KMP module for data and domain logic
- a desktop app shell using Compose Desktop
- a minimal app window to build on top of

## Run

```bash
./gradlew :app:run
```

## Structure

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
└── README.md
```

## Recommended next steps

1. Extract more logic from the Android repo into shared modules
2. Add media playback and queue management
3. Build the search/library views and navigation shell
4. Add Linux-native system integration such as tray and notifications
