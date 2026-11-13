plugins {
    kotlin("multiplatform")
}

// Architecture rules from ARCHITECTURE.md, checked against the source tree.
// The module has no production code; its tests read build files and sources.
kotlin {
    jvm("desktop")

    sourceSets {
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("moru.kmpRoot", rootDir.absolutePath)
    // Sources are inputs: a changed file must rerun the rules, not hit the cache.
    inputs.files(
        fileTree(rootDir) {
            include("*/src/**/*.kt", "*/*/src/**/*.kt", "**/build.gradle.kts")
            exclude("**/build/**", "architecture-test/**")
        },
    ).withPropertyName("architectureSources").withPathSensitivity(PathSensitivity.RELATIVE)
}
