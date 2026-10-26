plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
}

kotlin {
    jvm("desktop") {
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.components.resources)
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
