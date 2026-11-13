plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:model"))
                implementation(project(":core:domain"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val desktopMain by getting {
            dependencies {
                api("org.jetbrains.jediterm:jediterm-core:3.66")
                implementation("org.jetbrains.pty4j:pty4j:0.13.12")
                // Already shipped with pty4j; named here because the terminal host calls setsid through it.
                implementation("net.java.dev.jna:jna:5.14.0")
            }
        }
    }
}
