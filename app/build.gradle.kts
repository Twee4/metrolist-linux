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
                targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
                packageName = "metrolist-linux"
                packageVersion = "1.0.0"
            }
        }
    }
}
