pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

rootProject.name = "moru-desktop-kmp"

include(":app-desktop")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":core:terminal")
include(":core:runtime")
include(":core:system")
include(":feature:terminal")
include(":feature:workbench")
include(":feature:git")
include(":feature:codepanel")
include(":feature:settings")
include(":feature:notifications")
include(":feature:sessions")
include(":architecture-test")
