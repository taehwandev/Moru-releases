plugins {
    kotlin("multiplatform") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
    kotlin("jvm") version "2.3.20" apply false
    id("com.google.protobuf") version "0.10.0" apply false
    id("com.google.devtools.ksp") version "2.3.7" apply false
    id("androidx.room") version "2.8.4" apply false
    id("dev.zacsweers.metro") version "1.1.1" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
}

val test by tasks.registering {
    group = "verification"
    description = "Runs all configured desktop tests for the Moru KMP app."
}

// Classes named `*OsTest` drive the real OS pointer and keyboard, take app focus or read screen pixels, so they fail
// whenever someone uses the machine. They are kept out of `test`/`desktopTest` and run one module at a time by `osTest`.
val osTestPattern = "*OsTest"
val osTest by tasks.registering {
    group = "verification"
    description = "Runs the desktop tests that need the real mouse, keyboard, focus and screen. Leave the machine alone."
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        afterEvaluate {
            val desktopTest = tasks.findByName("desktopTest") as? Test ?: return@afterEvaluate
            desktopTest.filter.excludeTestsMatching(osTestPattern)
            val desktopOsTest = tasks.register<Test>("desktopOsTest") {
                group = "verification"
                description = "Runs this module's `$osTestPattern` classes against the real OS input and screen."
                testClassesDirs = desktopTest.testClassesDirs
                classpath = desktopTest.classpath
                filter {
                    includeTestsMatching(osTestPattern)
                    isFailOnNoMatchingTests = false
                }
            }
            rootProject.tasks.named("test") { dependsOn(desktopTest) }
            rootProject.tasks.named("osTest") { dependsOn(desktopOsTest) }
        }
    }
}

// Two modules moving the same pointer at once would break each other, so OS tests never overlap.
gradle.projectsEvaluated {
    subprojects.mapNotNull { it.tasks.findByName("desktopOsTest") }
        .zipWithNext { previous, next -> next.mustRunAfter(previous) }
}
