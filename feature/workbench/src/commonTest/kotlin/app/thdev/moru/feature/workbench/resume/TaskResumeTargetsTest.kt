package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskResumeTargetsTest {
    private fun session(task: String, command: String?, status: TerminalSessionStatus = TerminalSessionStatus.READY) =
        TerminalSessionSnapshot("s", TerminalSessionSpec(attachedItemId = task, startupCommand = command), status)

    @Test
    fun onlyTheSelectedTasksKnownAgentsAreOffered() {
        val sessions = listOf(
            session("mine", "claude --session-id c1"),
            session("mine", "codex --no-daemon"),
            session("other", "agy --conversation a1"),
        )

        assertEquals(listOf(TaskResumeTarget(AgentRuntime.Claude, "claude --resume c1")), taskResumeTargets(sessions, "mine"))
        assertTrue(taskResumeTargets(sessions, "empty-task").isEmpty())
    }

    @Test
    fun theLatestConversationWinsAndAnUnknownNewerOneBlocksTheOlderOne() {
        val old = session("mine", "claude --session-id old")
        val latest = session("mine", "claude --session-id new")

        assertEquals("claude --resume new", taskResumeTargets(listOf(old, latest), "mine").single().command)
        assertTrue(taskResumeTargets(listOf(old, latest, session("mine", "claude")), "mine").isEmpty())
    }

    @Test
    fun startingFailedAndNonTerminalSessionsCannotOfferResume() {
        val ready = session("mine", "claude --session-id c1")
        listOf(TerminalSessionStatus.STARTING, TerminalSessionStatus.FAILED).forEach { status ->
            assertTrue(taskResumeTargets(listOf(ready.copy(status = status)), "mine").isEmpty())
        }
        val preview = ready.copy(spec = ready.spec.copy(kind = TerminalSessionKind.WEB_PREVIEW))
        assertTrue(taskResumeTargets(listOf(preview), "mine").isEmpty())
        assertEquals(1, taskResumeTargets(listOf(ready.copy(status = TerminalSessionStatus.EXITED)), "mine").size)
    }
}
