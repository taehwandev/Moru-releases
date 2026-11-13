plugins {
    kotlin("multiplatform")
}

// Desktop OS integration: notifications, the Dock badge, and user-controlled result sharing.
// Setting MORU_SIGN_IDENTITY signs the helper with the hardened runtime, since notarization rejects unsigned code
// even inside a jar.
val signIdentity = providers.environmentVariable("MORU_SIGN_IDENTITY").orElse("")

// The result sharing picker is a separate, short-lived AppKit helper, signed when MORU_SIGN_IDENTITY is set.
val moruResultShareSources = layout.projectDirectory.dir("native/moru-result-share").asFileTree.matching { include("*.swift") }
val moruResultShareResources = layout.buildDirectory.dir("generated/moru-result-share")
val buildMoruResultShare = tasks.register<Exec>("buildMoruResultShare") {
    onlyIf { System.getProperty("os.name").lowercase().contains("mac") }
    inputs.files(moruResultShareSources)
    inputs.property("signIdentity", signIdentity)
    outputs.dir(moruResultShareResources)
    val output = moruResultShareResources.map { it.file("moru-result-share/moru-result-share").asFile }
    val identity = signIdentity
    doFirst { output.get().parentFile.mkdirs() }
    commandLine(
        listOf("xcrun", "swiftc", "-O", "-swift-version", "5") +
            moruResultShareSources.files.sortedBy { it.name }.map { it.absolutePath } +
            listOf("-o", output.get().absolutePath),
    )
    doLast {
        if (identity.get().isNotBlank()) {
            val signed = ProcessBuilder(
                "codesign", "--force", "--options", "runtime", "--timestamp", "--sign", identity.get(), output.get().absolutePath,
            ).inheritIO().start().waitFor()
            check(signed == 0) { "codesign failed for moru-result-share" }
        }
    }
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val desktopMain by getting {
            resources.srcDir(buildMoruResultShare.map { moruResultShareResources.get() })
        }
    }
}
