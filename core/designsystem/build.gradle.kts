plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

// AppKit materials render beneath the transparent desktop foreground. Ship the bridge with the UI module.
val backdropSources = layout.projectDirectory.dir("native/macos-backdrop").asFileTree.matching { include("*.swift") }
val backdropResources = layout.buildDirectory.dir("generated/macos-backdrop")
val backdropSignIdentity = providers.environmentVariable("MORU_SIGN_IDENTITY").orElse("")
val buildMacosBackdrop = tasks.register<Exec>("buildMacosBackdrop") {
    onlyIf { System.getProperty("os.name").lowercase().contains("mac") }
    inputs.files(backdropSources)
    inputs.property("signIdentity", backdropSignIdentity)
    outputs.dir(backdropResources)
    val output = backdropResources.map { it.file("macos-backdrop/libmoru-backdrop.dylib").asFile }
    val nativeArch = if (System.getProperty("os.arch") == "aarch64") "arm64" else "x86_64"
    doFirst {
        output.get().parentFile.mkdirs()
        commandLine(
            listOf("xcrun", "swiftc", "-O", "-swift-version", "5", "-target", "$nativeArch-apple-macos11.0", "-emit-library") +
                backdropSources.files.sortedBy { it.name }.map { it.absolutePath } +
                listOf("-o", output.get().absolutePath),
        )
    }
    doLast {
        if (backdropSignIdentity.get().isNotBlank()) {
            val result = ProcessBuilder(
                "codesign", "--force", "--options", "runtime", "--timestamp", "--sign",
                backdropSignIdentity.get(), output.get().absolutePath,
            ).inheritIO().start().waitFor()
            check(result == 0) { "codesign failed for the macOS backdrop bridge" }
        }
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty("compose.layers.type", "WINDOW")
    systemProperty("sun.java2d.metal", "false")
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:model"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                // Icons come from Lucide (ISC icons, MIT wrapper); screens use MoruIcon and never import it.
                implementation("com.composables:icons-lucide-cmp:2.2.1")
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }

        val desktopMain by getting {
            resources.srcDir(buildMacosBackdrop.map { backdropResources.get() })
            dependencies {
                implementation("net.java.dev.jna:jna:5.14.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
