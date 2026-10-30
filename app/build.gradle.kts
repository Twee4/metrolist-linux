import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinCompose)
}

kotlin {
    jvm("desktop") {
        withJava()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":shared"))
            }
        }

        // "desktop" is a custom target name, so Gradle's generated `desktopMain` shortcut is not
        // guaranteed to exist; `by getting` looks the source set up by name instead.
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "com.metrolist.linux.MainKt"

            nativeDistributions {
                // .deb and .rpm installers. (Compose's "AppImage" target only makes a plain portable
                // folder, not a .AppImage file; that comes from :app:createDistributable instead.)
                targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
                packageName = "metrolist-linux"
                packageVersion = "1.0.0"
                description = "Native Linux desktop client for YouTube Music"
                vendor = "Metrolist Linux"

                // The bundled runtime is trimmed to the modules it detects. This app loads libraries
                // reflectively (D-Bus, Ktor), so a trimmed runtime can fail only in the packaged build.
                // Including everything costs some size but removes that whole class of bug.
                includeAllModules = true

                linux {
                    iconFile.set(project.file("packaging/icon.png"))
                    shortcut = true // adds an entry to the desktop's application menu
                }
            }
        }
    }
}
