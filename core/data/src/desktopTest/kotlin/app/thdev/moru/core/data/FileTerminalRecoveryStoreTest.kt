package app.thdev.moru.core.data

import app.thdev.moru.core.model.SavedTerminal
import app.thdev.moru.core.model.WorkbenchLayout
import java.nio.file.Files
import okio.Path.Companion.toOkioPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class FileTerminalRecoveryStoreTest {
    @Test
    fun olderLayoutWritersCannotEraseCapturedIdsAndRemovedRowsAreNotResurrected() {
        val folder = Files.createTempDirectory("recovery-store").toFile()
        try {
            val recovery = FileTerminalRecoveryStore(folder.resolve("terminal-recovery.json").toOkioPath())
            val old = FileBackedWorkbenchLayoutRepository(folder.resolve("workbench-layout.json").toOkioPath())
            val current = defaultDesktopWorkbenchLayoutRepository(folder)
            val terminal = SavedTerminal(taskId = "task", hostKey = "host", workingDirectory = "/project")
            recovery.remember(mapOf("host" to "claude --resume exact"))
            old.saveLayout(WorkbenchLayout(terminals = listOf(terminal)))
            assertEquals(terminal.copy(startupCommand = "claude --resume exact"), current.loadLayout().terminals.single())
            val lab = terminal.copy(lab = true, startupCommand = "claude")
            old.saveLayout(WorkbenchLayout(terminals = listOf(lab)))
            assertEquals(lab, current.loadLayout().terminals.single())
            old.saveLayout(WorkbenchLayout())
            assertEquals(emptyList(), current.loadLayout().terminals)
        } finally { folder.deleteRecursively() }
    }

    @Test
    fun independentStoreInstancesMergeRecordsAndMalformedOriginalsArePreserved() {
        val folder = Files.createTempDirectory("recovery-store").toFile()
        try {
            val file = folder.resolve("terminal-recovery.json")
            val first = FileTerminalRecoveryStore(file.toOkioPath())
            val second = FileTerminalRecoveryStore(file.toOkioPath())
            first.remember(mapOf("a" to "claude --resume first"))
            second.remember(mapOf("b" to "codex --no-daemon resume second"))
            assertEquals(setOf("a", "b"), first.load().keys)
            file.writeText("unreadable original")
            assertFails { first.remember(mapOf("c" to "claude --resume third")) }
            assertEquals("unreadable original", file.readText())
        } finally { folder.deleteRecursively() }
    }
}
