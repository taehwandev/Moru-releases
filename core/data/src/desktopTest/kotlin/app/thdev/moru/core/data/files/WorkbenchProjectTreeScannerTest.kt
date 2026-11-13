package app.thdev.moru.core.data.files

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkbenchProjectTreeScannerTest {
    private fun project(): File = Files.createTempDirectory("tree-scan").toFile().apply {
        File(this, "src/main").mkdirs()
        File(this, "src/main/App.kt").writeText("fun main() {}")
        File(this, "build/out").mkdirs()
        File(this, "build/out/App.class").writeText("x")
        File(this, "local.properties").writeText("sdk.dir=/x")
        File(this, ".gitignore").writeText("build/\nlocal.properties\n")
    }

    private fun git(root: File, vararg args: String) {
        val process = ProcessBuilder(listOf("git", "-C", root.path) + args).redirectErrorStream(true).start()
        process.inputStream.readBytes()
        check(process.waitFor() == 0)
    }

    @Test
    fun listsEverythingButGitAndMarksWhatGitIgnores() {
        val root = project()
        git(root, "init", "-q")
        val entries = WorkbenchProjectTreeScanner.scan(root).associateBy { it.path }

        assertFalse(entries.keys.any { it == ".git" || it.startsWith(".git/") })
        assertTrue("src/main/App.kt" in entries)
        assertFalse(entries.getValue("src").isIgnored)
        assertTrue(entries.getValue("local.properties").isIgnored)
        val build = entries.getValue("build")
        assertTrue(build.isIgnored && build.isDeferred)
        assertFalse("build/out" in entries)
    }

    @Test
    fun deferredFolderChildrenAreReadOnOpenAndStayIgnored() {
        val root = project()
        git(root, "init", "-q")
        val children = WorkbenchProjectTreeScanner.children(root, "build")

        assertEquals(listOf("build/out"), children.map { it.path })
        assertTrue(children.single().isIgnored && children.single().isDeferred && children.single().depth == 1)
        assertTrue(WorkbenchProjectTreeScanner.children(root, "build/out").single().isIgnored)
        assertTrue(WorkbenchProjectTreeScanner.children(root, "../").isEmpty())
    }

    @Test
    fun outsideARepositoryBuildFoldersStandInForTheIgnoreRules() {
        val root = project()
        val entries = WorkbenchProjectTreeScanner.scan(root).associateBy { it.path }

        assertTrue(entries.getValue("build").isIgnored)
        assertFalse(entries.getValue("local.properties").isIgnored)
    }
}
