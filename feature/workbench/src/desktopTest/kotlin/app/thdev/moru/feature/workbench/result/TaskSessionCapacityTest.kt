package app.thdev.moru.feature.workbench.result

import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.Maximum
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.resume.startResumeTerminal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TaskSessionCapacityTest {
    @Test
    fun buildsAdmitTheLastSessionAndCannotReplaceOrSelectAtCapacity() {
        val manager = InMemoryTerminalSessionManager(List(Maximum - 1) { TerminalSessionSpec(attachedItemId = "task") })
        assertNotNull(startBuildTerminal(manager, "task", "/folder", "Last build", "./gradlew check"))
        manager.selectSession(manager.listSessions().first().id)
        val before = manager.listSessions()
        val selected = manager.selectedSessionId()
        assertNull(startBuildTerminal(manager, "task", "/folder", "Rejected build", "must not start"))
        assertEquals(before, manager.listSessions())
        assertEquals(selected, manager.selectedSessionId())
    }

    @Test
    fun resumeCannotCreateATerminalPastTheMaximumButBecomesAvailableAfterClose() {
        val manager = InMemoryTerminalSessionManager(listOf(
            TerminalSessionSpec(attachedItemId = "task", startupCommand = "claude --session-id conversation"),
        ) + List(Maximum - 1) { TerminalSessionSpec(attachedItemId = "task") })
        val before = manager.listSessions()
        val selected = manager.selectedSessionId()
        assertNull(startResumeTerminal(manager, "task", "/folder", AgentRuntime.Claude, "Rejected resume"))
        assertEquals(before, manager.listSessions())
        assertEquals(selected, manager.selectedSessionId())
        manager.closeSession(before.last().id)
        assertNotNull(startResumeTerminal(manager, "task", "/folder", AgentRuntime.Claude, "Last resume"))
        assertEquals(Maximum, manager.listSessions().size)
        assertEquals("task", manager.listSessions().last().spec.attachedItemId)
    }
}
