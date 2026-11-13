import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty("compose.layers.type", "WINDOW")
}

// Terminal sessions as the sidebar shows them: their activity, rows, the list of every session and its strings.
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

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // UI tests render the rows and the popover headlessly on the desktop JVM.
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}
