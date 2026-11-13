plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm("desktop")
    macosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:model"))
            }
        }
        val commonTest by getting {
            dependencies { implementation(kotlin("test")) }
        }
    }
}
