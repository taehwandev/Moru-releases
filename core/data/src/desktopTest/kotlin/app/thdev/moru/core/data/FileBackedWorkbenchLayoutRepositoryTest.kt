package app.thdev.moru.core.data

import app.thdev.moru.core.model.SavedResultPane
import app.thdev.moru.core.model.SavedTerminal
import app.thdev.moru.core.model.WorkbenchLayout
import app.thdev.moru.core.model.panel.PanelLayouts
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class FileBackedWorkbenchLayoutRepositoryTest {
    @Test
    fun aMissingOrBrokenFileReadsAsAnEmptyLayout() {
        val dir = Files.createTempDirectory("moru-layout")
        try {
            val path = dir.resolve("workbench-layout.json")
            assertEquals(WorkbenchLayout(), FileBackedWorkbenchLayoutRepository(path.toOkioPath()).loadLayout())

            path.writeText("{ not json")
            assertEquals(WorkbenchLayout(), FileBackedWorkbenchLayoutRepository(path.toOkioPath()).loadLayout())
            // The unreadable file is kept beside the layout, not overwritten by the next save.
            assertEquals("{ not json", Files.readString(dir.resolve("workbench-layout.json.unreadable")))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun overlappingSavesNeverLeaveAPartialFile() {
        val dir = Files.createTempDirectory("moru-layout")
        try {
            val path = dir.resolve("workbench-layout.json")
            val layouts = (1..8).map { n -> WorkbenchLayout(collapsedProjectIds = List(200) { "p$n-$it" }) }
            val threads = layouts.map { layout ->
                Thread { repeat(20) { FileBackedWorkbenchLayoutRepository(path.toOkioPath()).saveLayout(layout) } }
            }
            threads.forEach(Thread::start)
            threads.forEach(Thread::join)

            val loaded = FileBackedWorkbenchLayoutRepository(path.toOkioPath()).loadLayout()
            kotlin.test.assertTrue(loaded in layouts)
            assertEquals(listOf("workbench-layout.json"), Files.list(dir).map { it.fileName.toString() }.toList())
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
