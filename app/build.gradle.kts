plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
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

        desktopMain {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.icons.extended)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "com.metrolist.linux.MainKt"
            jvmArgs += listOf("-Dsun.java2d.metal=false")
        }
    }
}
