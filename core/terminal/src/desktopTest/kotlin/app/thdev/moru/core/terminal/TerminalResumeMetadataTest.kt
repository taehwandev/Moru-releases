package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalResumeMetadataTest {
    @Test
    fun recordingResumeChangesOnlyMetadataAndRejectsStaleKeysAndOtherKinds() {
        listOf(InMemoryTerminalSessionManager(), DesktopPtyTerminalSessionManager(spawnProcesses = false)).forEach { manager ->
            val terminal = manager.createSession(TerminalSessionSpec(hostKey = "host", attachedItemId = "task"))
            val before = manager.listSessions().single()
            val revision = manager.revision()
            assertFalse(manager.recordResumeCommand(terminal.id, "wrong", "claude --resume exact"))
            assertTrue(manager.recordResumeCommand(terminal.id, "host", "claude --resume exact"))
            assertEquals(before.copy(spec = before.spec.copy(startupCommand = "claude --resume exact")), manager.listSessions().single())
            assertEquals(revision + 1, manager.revision())
            assertEquals(terminal.id, manager.selectedSessionId())
            assertFalse(manager.recordResumeCommand(terminal.id, "host", "claude --resume exact"))
            assertFalse(manager.recordResumeCommand(terminal.id, "host", " "))
            val lab = manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.LAB, hostKey = "lab"))
            assertFalse(manager.recordResumeCommand(lab.id, "lab", "claude --resume exact"))
            manager.closeSession(terminal.id)
            assertFalse(manager.recordResumeCommand(terminal.id, "host", "claude --resume exact"))
        }
    }
}
