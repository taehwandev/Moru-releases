package app.thdev.moru.app

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoruProjectRootResolutionTest {
    @Test
    fun anAppBuiltInsideARepositoryDoesNotMakeThatRepositoryTheProject() {
        // The packaged app sits in the repository it was built from; that says nothing about what the user opens.
        val repoRoot = Files.createTempDirectory("moru-repo-root").toFile()
        repoRoot.resolve("desktop-kmp/core/workflow").mkdirs()
        val home = repoRoot.resolve("home").apply { mkdirs() }

        try {
            for (cwd in listOf(File("/"), repoRoot, repoRoot.resolve("desktop-kmp"))) {
                val launch = resolveMoruProjectLaunch(emptyArray(), null, cwd, home)

                assertNull(launch.root, "cwd $cwd must not be taken as a project")
                assertTrue(!launch.explicit)
            }
            assertEquals(home.resolve(".moru-community/workspace").absoluteFile, resolveMoruProjectRoot(null, repoRoot, home))
        } finally {
            repoRoot.deleteRecursively()
        }
    }

    @Test
    fun resolvesPlatformApplicationSupportDirectoryForCatalogOnMac() {
        val home = File("/Users/tester")

        val resolved = resolveMoruAppDataDir(
            osName = "Mac OS X",
            userHome = home,
            appDataOverride = null,
            development = false,
        )

        assertEquals(home.resolve("Library/Application Support/Moru Community").absoluteFile, resolved)
    }

    @Test
    fun aDevelopmentBuildKeepsItsOwnFolderApartFromTheInstalledMoru() {
        val home = File("/Users/tester")

        val mac = resolveMoruAppDataDir(osName = "Mac OS X", userHome = home, appDataOverride = null, development = true)
        val linux = resolveMoruAppDataDir(osName = "Linux", userHome = home, appDataOverride = null, development = true)

        assertEquals(home.resolve("Library/Application Support/Moru Community Dev").absoluteFile, mac)
        assertTrue(linux.path.endsWith("Moru Community Dev"))
        // An explicit folder still wins, so a test or a one-off run can point anywhere.
        assertEquals(
            File("/custom/dev").absoluteFile,
            resolveMoruAppDataDir(osName = "Mac OS X", userHome = home, appDataOverride = "/custom/dev", development = true),
        )
    }

    @Test
    fun appDataOverrideWinsWhenProvided() {
        val resolved = resolveMoruAppDataDir(
            osName = "Mac OS X",
            userHome = File("/Users/tester"),
            appDataOverride = "/custom/moru-data",
        )

        assertEquals(File("/custom/moru-data").absoluteFile, resolved)
    }

    @Test
    fun fallsBackToUserWorkspaceWhenNoRepositoryMarkerExists() {
        val home = Files.createTempDirectory("moru-home").toFile()

        try {
            val resolved = resolveMoruProjectRoot(
                configuredPath = null,
                userDir = File("/"),
                userHome = home,
            )

            assertEquals(home.resolve(".moru-community/workspace").absoluteFile, resolved)
        } finally {
            home.deleteRecursively()
        }
    }

    @Test
    fun acceptsExternalRelativeFolderWithSpacesFromTerminal() {
        val cwd = Files.createTempDirectory("moru-launch").toFile()
        val project = cwd.resolve("my project").apply { mkdirs() }
        try {
            for (arguments in listOf(arrayOf("my project"), arrayOf("--project", "my project"), arrayOf("--project=my project"))) {
                val launch = resolveMoruProjectLaunch(arguments, null, cwd, cwd.resolve("home"))

                assertEquals(project.canonicalFile, launch.root)
                assertTrue(launch.explicit)
                assertNull(launch.notice)
            }
        } finally {
            cwd.deleteRecursively()
        }
    }

    @Test
    fun terminalFolderTakesPriorityOverConfiguredDefault() {
        val cwd = Files.createTempDirectory("moru-launch-priority").toFile()
        val project = cwd.resolve("requested").apply { mkdirs() }
        val defaultProject = cwd.resolve("default").apply { mkdirs() }
        try {
            val launch = resolveMoruProjectLaunch(
                arguments = arrayOf("--project", project.path),
                configuredPath = defaultProject.path,
                userDir = cwd,
                userHome = cwd.resolve("home"),
            )

            assertEquals(project.canonicalFile, launch.root)
        } finally {
            cwd.deleteRecursively()
        }
    }

    @Test
    fun invalidExplicitFolderDoesNotFallBackToCurrentDirectory() {
        val cwd = Files.createTempDirectory("moru-invalid-launch").toFile()
        val regularFile = cwd.resolve("file.txt").apply { writeText("not a directory") }
        try {
            for (path in listOf(cwd.resolve("missing").path, regularFile.path, "")) {
                val launch = resolveMoruProjectLaunch(arrayOf("--project", path), null, cwd, cwd)

                assertNull(launch.root)
                assertTrue(launch.explicit)
                assertTrue(launch.notice.orEmpty().contains("existing, readable folder"))
            }
        } finally {
            cwd.deleteRecursively()
        }
    }

    @Test
    fun missingProjectArgumentProducesUsageNotice() {
        val launch = resolveMoruProjectLaunch(arrayOf("--project"), configuredPath = "/")

        assertNull(launch.root)
        assertTrue(launch.notice.orEmpty().contains("Moru --project <folder>"))
    }

    @Test
    fun emptyGuiLaunchHasNoProjectCandidate() {
        val home = Files.createTempDirectory("moru-empty-gui").toFile()
        try {
            for (cwd in listOf(File("/"), home)) {
                val launch = resolveMoruProjectLaunch(emptyArray(), null, cwd, home)

                assertNull(launch.root)
                assertNull(launch.notice)
            }
        } finally {
            home.deleteRecursively()
        }
    }

    @Test
    fun aTerminalFolderWithoutAnArgumentIsNotAProjectEither() {
        val cwd = Files.createTempDirectory("moru-some-folder").toFile()
        try {
            val launch = resolveMoruProjectLaunch(emptyArray(), null, cwd, cwd.resolve("home"))

            assertNull(launch.root)
            assertNull(launch.notice)
        } finally {
            cwd.deleteRecursively()
        }
    }
}
