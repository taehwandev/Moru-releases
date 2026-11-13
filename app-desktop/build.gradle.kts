import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.IsoFields
import org.gradle.api.tasks.JavaExec
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("dev.zacsweers.metro")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Setting MORU_SIGN_IDENTITY (the name after "Developer ID Application: ") signs the app and its bundled runtime.
// Unset, builds stay unsigned.
val signIdentity = providers.environmentVariable("MORU_SIGN_IDENTITY").orElse("")
val macEntitlements = layout.projectDirectory.file("macos-entitlements.plist")

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:data"))
                implementation(project(":core:domain"))
                implementation(project(":core:model"))
                implementation(project(":core:terminal"))
                implementation(project(":core:runtime"))
                implementation(project(":core:designsystem"))
                implementation(project(":core:ui"))
                implementation(project(":core:system"))
                implementation(project(":feature:terminal"))
                implementation(project(":feature:workbench"))
                implementation(project(":feature:git"))
                implementation(project(":feature:codepanel"))
                implementation(project(":feature:settings"))
                implementation(project(":feature:notifications"))
                implementation(project(":feature:sessions"))
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

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // Talks to AppKit to set the whole application's look on macOS; the terminal already brings it in.
                implementation("net.java.dev.jna:jna:5.14.0")
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation("androidx.room:room-runtime:2.8.4")
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}

fun defaultMoruVersion(): String {
    val today = LocalDate.now(ZoneOffset.UTC)
    val weekBasedYear = today.get(IsoFields.WEEK_BASED_YEAR) % 100
    val week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    return "%02d.%02d.0".format(weekBasedYear, week)
}

val moruVersion = providers.gradleProperty("moruVersion")
    .orElse(providers.environmentVariable("MORU_VERSION"))
    .orElse(defaultMoruVersion())
// Embed the packaging version in every runtime, including Gradle run and standalone jars.
val buildVersionSources = layout.buildDirectory.dir("generated/build-version")
val generateBuildVersion = tasks.register("generateBuildVersion") {
    inputs.property("version", moruVersion)
    outputs.dir(buildVersionSources)
    doLast {
        val escaped = moruVersion.get().replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("$", "\\$").replace("\n", "\\n").replace("\r", "\\r")
        val target = buildVersionSources.get().file("app/thdev/moru/app/MoruBuildVersion.kt").asFile
        target.parentFile.mkdirs()
        target.writeText("package app.thdev.moru.app\n\ninternal object MoruBuildVersion { const val value = \"$escaped\" }\n")
    }
}
kotlin.sourceSets.named("commonMain") { kotlin.srcDir(generateBuildVersion.map { buildVersionSources.get() }) }
val moruIconIcns = layout.projectDirectory.file("src/desktopMain/resources/icon.icns")
val moruDevelopmentIcon = layout.projectDirectory.file("src/desktopMain/resources/icon-dev.png")

version = moruVersion.get()

compose.desktop {
    application {
        mainClass = "app.thdev.moru.app.MainKt"
        // Left to its defaults the JVM starts with 1/64 and may grow to 1/4 of the machine's memory, so streaming
        // terminal output alone pushes Moru to gigabytes before it collects. Percentages, not -Xms/-Xmx: the packaged
        // terminal host runs through this same launcher and sets its own smaller heap with -Xmx, which wins over them.
        // MaxRAM makes the JVM size the heap as if the machine had at most 8GB, so the cap is 1GB on bigger machines
        // instead of 12.5% of all memory; a live heap of 70-160MB leaves plenty of room under it.
        jvmArgs += listOf(
            "-XX:MaxRAM=8g",
            "-XX:InitialRAMPercentage=0.5",
            "-XX:MaxRAMPercentage=12.5",
            "-XX:G1PeriodicGCInterval=60000",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Moru Community"
            packageVersion = moruVersion.get()
            // What `suggestRuntimeModules` lists for the app (java.net.http carries the web Preview's DevTools socket,
            // java.naming/java.sql the emulator gRPC line), plus jdk.jfr kept from before.
            modules("java.instrument", "java.naming", "java.net.http", "java.sql", "jdk.jfr", "jdk.unsupported")

            macOS {
                packageName = "Moru Community"
                bundleID = "app.thdev.moru.community"
                dockName = "Moru Community"
                iconFile.set(moruIconIcns)
                if (signIdentity.get().isNotBlank()) {
                    signing {
                        sign.set(true)
                        identity.set(signIdentity.get())
                    }
                    entitlementsFile.set(macEntitlements)
                }
            }
        }
    }
}

// pty4j ships its spawn helper inside the jar signed without the hardened runtime, which the notary rejects, and
// Compose re-signs only .dylib/.jnilib entries. A signed build re-signs that entry and then re-seals the app.
val resignNestedCode = tasks.register("resignNestedCode") {
    onlyIf { signIdentity.get().isNotBlank() }
    val identity = signIdentity
    val app = layout.buildDirectory.dir("compose/binaries/main/app/Moru Community.app")
    val work = layout.buildDirectory.dir("tmp/resign")
    val entitlements = macEntitlements
    doLast {
        fun run(vararg command: String, dir: File? = null) {
            val exit = ProcessBuilder(*command).directory(dir).inheritIO().start().waitFor()
            check(exit == 0) { "failed: ${command.joinToString(" ")}" }
        }
        val spawnHelper = "resources/com/pty4j/native/darwin/pty4j-unix-spawn-helper"
        val scratch = work.get().asFile.apply { deleteRecursively(); mkdirs() }
        app.get().asFile.resolve("Contents/app").listFiles().orEmpty()
            .filter { it.name.startsWith("pty4j-") && it.extension == "jar" }
            .forEach { jar ->
                run("unzip", "-o", "-q", jar.path, spawnHelper, "-d", scratch.path)
                run("codesign", "--force", "--options", "runtime", "--timestamp", "--sign", identity.get(), scratch.resolve(spawnHelper).path)
                run("zip", "-q", jar.path, spawnHelper, dir = scratch)
            }
        run(
            "codesign", "--force", "--options", "runtime", "--timestamp",
            "--entitlements", entitlements.asFile.path, "--sign", identity.get(), app.get().asFile.path,
        )
    }
}
tasks.matching { it.name == "createDistributable" }.configureEach { finalizedBy(resignNestedCode) }
tasks.matching { it.name == "packageDmg" }.configureEach { dependsOn(resignNestedCode) }

// JavaExec normally reads mutable compilation outputs lazily. A second build can replace classes beneath a running
// app, so each development launch keeps its project outputs outside build/ until that process exits.
var developmentRuntimeClasspathSnapshot: File? = null
val cleanDevelopmentRuntimeClasspath = tasks.register("cleanDevelopmentRuntimeClasspath") {
    doLast {
        developmentRuntimeClasspathSnapshot?.let { snapshot ->
            check(snapshot.deleteRecursively()) { "Could not remove the development runtime snapshot" }
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        systemProperty("moru.development", "true")
        finalizedBy(cleanDevelopmentRuntimeClasspath)
        doFirst {
            val snapshots = rootProject.layout.projectDirectory.dir(".gradle/moru-dev-runtimes").asFile
            snapshots.mkdirs()
            val snapshot = Files.createTempDirectory(snapshots.toPath(), "run-").toFile()
            developmentRuntimeClasspathSnapshot = snapshot
            val dependencyCache = gradle.gradleUserHomeDir.resolve("caches").canonicalFile.toPath()
            val runtimeClasspath = classpath.files.filter { it.exists() }.mapIndexed { index, source ->
                // Dependency-cache artifacts are keyed by content; project classes, resources and jars are mutable.
                if (source.isFile && source.canonicalFile.toPath().startsWith(dependencyCache)) {
                    source
                } else {
                    val target = snapshot.resolve("$index-${source.name}")
                    check(source.copyRecursively(target)) { "Could not snapshot the development runtime classpath" }
                    target
                }
            }
            classpath = files(runtimeClasspath)
        }
        if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
            doFirst {
                jvmArgs("-Xdock:name=Moru Community Dev", "-Xdock:icon=${moruDevelopmentIcon.asFile.absolutePath}")
            }
        }
    }
}

// Match Main before any test initializes Compose; changing layers after startup is too late.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty("compose.layers.type", "WINDOW")
}
