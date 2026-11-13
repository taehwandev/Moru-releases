package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class CliUpdateRestartStepTest {
    private val updatedAt = 1_000_000L
    private val now = updatedAt + 120_000

    private fun session(
        command: String? = "claude --session-id 7f1c 'fix the build'",
        startedAt: Long? = updatedAt - 600_000,
        lastOutput: Long? = now - 60_000,
    ) = TerminalSessionSnapshot(
        id = "term-1",
        spec = TerminalSessionSpec(attachedItemId = "task", startupCommand = command),
        status = TerminalSessionStatus.READY,
        startedAtMillis = startedAt,
        lastOutputAtMillis = lastOutput,
    )

    private fun step(
        snapshot: TerminalSessionSnapshot = session(),
        activity: SessionActivity = SessionActivity.Waiting,
        running: Boolean? = true,
        updated: Long? = updatedAt,
    ) = cliUpdateRestartStep(snapshot, activity, running, updated, now)

    @Test
    fun aQuietAgentWithAKnownConversationIsRestartedAtOnce() {
        assertEquals(CliUpdateRestartStep.RestartNow, step())
        assertEquals(CliUpdateRestartStep.RestartNow, step(activity = SessionActivity.Quiet))
        assertEquals(CliUpdateRestartStep.RestartNow, step(session(command = "codex --no-daemon resume 019a-77 || codex --no-daemon")))
    }

    @Test
    fun aBusyOrAskingAgentWaitsForItsTurnToEnd() {
        assertEquals(CliUpdateRestartStep.Wait, step(activity = SessionActivity.Working))
        assertEquals(CliUpdateRestartStep.Wait, step(activity = SessionActivity.Asking))
        assertEquals(CliUpdateRestartStep.Wait, step(session(lastOutput = now - 5_000), activity = SessionActivity.Quiet))
        assertEquals(CliUpdateRestartStep.Wait, step(running = null))
    }

    @Test
    fun anAgentWhoseConversationIsUnknownIsLeftToTheUser() {
        assertEquals(CliUpdateRestartStep.AskUser, step(session(command = "codex --no-daemon 'add tests'")))
        assertEquals(CliUpdateRestartStep.AskUser, step(session(command = "agy")))
    }

    @Test
    fun sessionsAlreadyOnTheNewVersionOrNotRunningAnAgentAreDone() {
        assertEquals(CliUpdateRestartStep.Done, step(updated = null))
        assertEquals(CliUpdateRestartStep.Done, step(session(startedAt = updatedAt + 1)))
        assertEquals(CliUpdateRestartStep.Done, step(running = false))
        assertEquals(CliUpdateRestartStep.Done, step(session(command = null)))
        assertEquals(CliUpdateRestartStep.Done, step(session(command = "npm run dev")))
    }

    @Test
    fun theResumeLineIsTypedOnlyAfterTheAgentReallyQuit() = runBlocking {
        val typed = mutableListOf<String>()
        var running = true
        val restarted = restartAgentInSession(
            sessionId = "term-1",
            command = "claude --resume 7f1c || claude --session-id 7f1c",
            submitLine = { _, line -> typed += line; true },
            stopProgram = { running = false; true },
            programRunning = { running },
        )
        assertTrue(restarted)
        assertEquals(listOf("claude --resume 7f1c || claude --session-id 7f1c"), typed)
    }

    @Test
    fun nothingIsTypedIntoAnAgentThatDidNotQuit() = runBlocking {
        val typed = mutableListOf<String>()
        assertFalse(
            restartAgentInSession("term-1", "claude --resume 7f1c", { _, line -> typed += line; true },
                stopProgram = { false }, programRunning = { true }),
        )
        assertTrue(typed.isEmpty())
    }
}
