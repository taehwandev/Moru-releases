plugins {
    kotlin("multiplatform")
    id("androidx.room")
    id("com.google.devtools.ksp")
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:model"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("androidx.room:room-runtime:2.8.4")
                implementation("androidx.sqlite:sqlite-bundled:2.6.2")
                implementation("com.squareup.okio:okio:3.9.0")
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

dependencies {
    add("kspDesktop", "androidx.room:room-compiler:2.8.4")
}

room {
    schemaDirectory("$projectDir/schemas")
}
