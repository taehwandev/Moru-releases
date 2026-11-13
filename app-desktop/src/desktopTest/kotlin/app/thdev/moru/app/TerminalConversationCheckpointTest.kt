package app.thdev.moru.app

import app.thdev.moru.core.data.defaultDesktopWorkbenchLayoutRepository
import app.thdev.moru.core.data.defaultDesktopTerminalRecoveryStore
import app.thdev.moru.core.data.InMemoryWorkbenchLayoutRepository
import app.thdev.moru.core.model.AgentConversationRef
import app.thdev.moru.core.model.WorkbenchLayout
import app.thdev.moru.core.terminal.DesktopPtyTerminalSessionManager
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.workbench.restore.restoreTerminals
import app.thdev.moru.feature.workbench.restore.savedTerminalsOf
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalConversationCheckpointTest {
    @Test
    fun runningAgentsInDifferentTasksOfOneFolderSurviveWithoutAnyQuitCallback() = runBlocking {
        val folder = Files.createTempDirectory("terminal-recovery").toFile()
        try {
            val repository = defaultDesktopWorkbenchLayoutRepository(folder)
            val sessions = DesktopPtyTerminalSessionManager(spawnProcesses = false)
            val first = sessions.createSession(TerminalSessionSpec(attachedItemId = "task-a", workingDirectory = "/same"))
            val second = sessions.createSession(TerminalSessionSpec(attachedItemId = "task-b", workingDirectory = "/same"))
            val keeper = WorkbenchLayoutKeeper(repository, saveDelayMillis = 60_000)
            val ids = mapOf(first.id to "thread-a", second.id to "thread-b")
            val saved = mutableMapOf<String, String>()

            checkpointTerminalConversations(sessions, keeper, { AgentConversationRef("codex", ids.getValue(it)) }, saved::putAll)

            // Recreate every in-memory owner, as after a forced quit or reboot; no old host or process is involved.
            val after = InMemoryTerminalSessionManager()
            restoreTerminals(after, defaultDesktopWorkbenchLayoutRepository(folder).loadLayout())
            assertEquals(listOf("task-a", "task-b"), after.listSessions().map { it.spec.attachedItemId })
            assertEquals(listOf("codex --no-daemon resume thread-a", "codex --no-daemon resume thread-b"),
                after.listSessions().map { it.spec.startupCommand })
            assertEquals(sessions.listSessions().map { it.spec.hostKey }, after.listSessions().map { it.spec.hostKey })
            assertEquals(2, saved.size)
            assertEquals(second.id, sessions.selectedSessionId())
        } finally { folder.deleteRecursively() }
    }

    @Test
    fun anUnavailableReadingDoesNotEraseTheLastKnownConversationAndLabIsNotProbed() = runBlocking {
        val sessions = InMemoryTerminalSessionManager()
        sessions.createSession(TerminalSessionSpec(hostKey = "known", startupCommand = "claude --resume exact"))
        val lab = sessions.createSession(TerminalSessionSpec(kind = TerminalSessionKind.LAB, hostKey = "lab", startupCommand = "claude"))
        val repository = InMemoryWorkbenchLayoutRepository(WorkbenchLayout(terminals = savedTerminalsOf(sessions.listSessions())))
        val keeper = WorkbenchLayoutKeeper(repository)
        checkpointTerminalConversations(sessions, keeper, { id ->
            check(id != lab.id)
            AgentConversationRef("claude", null)
        })
        assertEquals("claude --resume exact", sessions.listSessions().first().spec.startupCommand)
        assertEquals("claude", sessions.listSessions().last().spec.startupCommand)
    }

    @Test
    fun aSessionClosedDuringInspectionIsNeverAddedBackAndAMoveUsesItsCurrentTask() = runBlocking {
        val sessions = DesktopPtyTerminalSessionManager(spawnProcesses = false)
        val closed = sessions.createSession(TerminalSessionSpec(attachedItemId = "old"))
        val moved = sessions.createSession(TerminalSessionSpec(attachedItemId = "old"))
        val repository = InMemoryWorkbenchLayoutRepository()
        val saved = mutableMapOf<String, String>()
        checkpointTerminalConversations(sessions, WorkbenchLayoutKeeper(repository), { id ->
            if (id == closed.id) sessions.closeSession(id) else sessions.moveSessionToTask(id, "new")
            AgentConversationRef("claude", "exact")
        }, saved::putAll)
        assertEquals(listOf("new"), repository.loadLayout().terminals.map { it.taskId })
        assertEquals(setOf(moved.spec.hostKey), saved.keys)
    }

    @Test
    fun unsafeIdsAndFailedInspectionAreNotStoredAndSidecarFailuresCanRetry() = runBlocking {
        val sessions = InMemoryTerminalSessionManager()
        val session = sessions.createSession(TerminalSessionSpec(hostKey = "host"))
        val keeper = WorkbenchLayoutKeeper(InMemoryWorkbenchLayoutRepository())
        checkpointTerminalConversations(sessions, keeper, { AgentConversationRef("claude", "x; echo unsafe") })
        assertNull(sessions.listSessions().single().spec.startupCommand)
        checkpointTerminalConversations(sessions, keeper, { error("unavailable") })
        assertNull(sessions.listSessions().single().spec.startupCommand)
        checkpointTerminalConversations(sessions, keeper, { AgentConversationRef("claude", "exact") }, { error("temporary write failure") })
        assertNull(sessions.listSessions().single().spec.startupCommand)
        val saved = mutableMapOf<String, String>()
        checkpointTerminalConversations(sessions, keeper, { AgentConversationRef("claude", "exact") }, saved::putAll)
        assertEquals(mapOf(requireNotNull(session.spec.hostKey) to "claude --resume exact"), saved)
    }

    @Test
    fun aDetectedProviderSwitchWithoutAnIdCannotRestoreThePreviousProvidersConversation() = runBlocking {
        val folder = Files.createTempDirectory("provider-switch").toFile()
        try {
            val sessions = DesktopPtyTerminalSessionManager(spawnProcesses = false)
            sessions.createSession(TerminalSessionSpec(attachedItemId = "task"))
            val repository = defaultDesktopWorkbenchLayoutRepository(folder)
            val recovery = defaultDesktopTerminalRecoveryStore(folder)
            val keeper = WorkbenchLayoutKeeper(repository)
            checkpointTerminalConversations(sessions, keeper, { AgentConversationRef("claude", "old") }, recovery::remember)
            checkpointTerminalConversations(sessions, keeper, { AgentConversationRef("codex", null) }, recovery::remember)
            val restored = InMemoryTerminalSessionManager()
            restoreTerminals(restored, defaultDesktopWorkbenchLayoutRepository(folder).loadLayout())
            assertEquals("codex --no-daemon", restored.listSessions().single().spec.startupCommand)
        } finally { folder.deleteRecursively() }
    }

    @Test
    fun aFailedSidecarWriteNeverPublishesANewerLayoutThatTheOldSidecarWouldRollBack() = runBlocking {
        val folder = Files.createTempDirectory("checkpoint-order").toFile()
        try {
            val sessions = DesktopPtyTerminalSessionManager(spawnProcesses = false)
            val session = sessions.createSession(TerminalSessionSpec(attachedItemId = "task"))
            val repository = defaultDesktopWorkbenchLayoutRepository(folder)
            val recovery = defaultDesktopTerminalRecoveryStore(folder)
            val keeper = WorkbenchLayoutKeeper(repository, saveDelayMillis = 0)
            checkpointTerminalConversations(sessions, keeper, { AgentConversationRef("claude", "A") }, recovery::remember)
            checkpointTerminalConversations(sessions, keeper, { AgentConversationRef("claude", "B") }, {
                keeper.flush()
                assertEquals("claude --resume A", sessions.listSessions().single().spec.startupCommand)
                error("sidecar write failed before publishing B")
            })
            assertEquals("claude --resume A", repository.loadLayout().terminals.single().startupCommand)
            checkpointTerminalConversations(sessions, keeper, { AgentConversationRef("claude", "B") }, recovery::remember)
            assertEquals("claude --resume B", repository.loadLayout().terminals.single().startupCommand)
            assertEquals("claude --resume B", recovery.load()[session.spec.hostKey])
        } finally { folder.deleteRecursively() }
    }
}
