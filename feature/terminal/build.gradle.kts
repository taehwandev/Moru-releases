plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:model"))
                implementation(project(":core:terminal"))
                implementation(project(":core:designsystem"))
                implementation(project(":core:ui"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("org.jetbrains.jediterm:jediterm-ui:3.66")
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                // A real Compose window in a test needs the native rendering library.
                implementation(compose.desktop.currentOs)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}
