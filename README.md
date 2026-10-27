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
