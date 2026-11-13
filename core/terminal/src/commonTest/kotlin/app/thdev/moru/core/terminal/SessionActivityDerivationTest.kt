package app.thdev.moru.core.terminal

import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionActivityDerivationTest {
    private val now = 1_000_000L

    private fun snapshot(
        status: TerminalSessionStatus = TerminalSessionStatus.READY,
        lastOutputAtMillis: Long? = null,
        exitCode: Int? = null,
        startedAtMillis: Long? = null,
    ) = TerminalSessionSnapshot(
        id = "term-1",
        spec = TerminalSessionSpec(title = "claude"),
        status = status,
        lastOutputAtMillis = lastOutputAtMillis,
        exitCode = exitCode,
        startedAtMillis = startedAtMillis,
    )

    @Test
    fun aQuestionOnScreenIsAskingAtOnceEvenWhileItRedrawsButNotOnceTheShellIsBack() {
        val asks: (String) -> Boolean = { true }
        val running: (String) -> Boolean? = { true }
        assertEquals(
            SessionActivity.Asking,
            deriveSessionActivity(snapshot(lastOutputAtMillis = now - 1_000), now, hasRunningProgram = running, asksUser = asks),
        )
        assertEquals(
            SessionActivity.Idle,
            deriveSessionActivity(snapshot(lastOutputAtMillis = now - 1_000), now, hasRunningProgram = { false }, asksUser = asks),
        )
        assertEquals(
            SessionActivity.Failed,
            deriveSessionActivity(snapshot(status = TerminalSessionStatus.EXITED, exitCode = 1), now, asksUser = asks),
        )
    }

    @Test
    fun anAgentWhoseScreenSaysItIsAtWorkIsWorkingThroughASilenceAndItsQuestionStillComesFirst() {
        val running: (String) -> Boolean? = { true }
        val silent = snapshot(lastOutputAtMillis = now - 60_000)
        assertEquals(SessionActivity.Quiet, deriveSessionActivity(silent, now, hasRunningProgram = running, asksUser = { false }, showsBusy = { false }))
        assertEquals(SessionActivity.Working, deriveSessionActivity(silent, now, hasRunningProgram = running, asksUser = { false }, showsBusy = { true }))
        assertEquals(SessionActivity.Asking, deriveSessionActivity(silent, now, hasRunningProgram = running, asksUser = { true }, showsBusy = { true }))
    }

    @Test
    fun anUnseenTurnDoesNotLoseAttentionAfterFiveMinutes() {
        val running: (String) -> Boolean? = { true }
        fun after(millis: Long) = deriveSessionActivity(
            snapshot(lastOutputAtMillis = now - millis), now, hasRunningProgram = running, asksUser = { false }, showsBusy = { false },
        )
        assertEquals(SessionActivity.Quiet, after(SESSION_DONE_LINGER_MILLIS))
        assertEquals(SessionActivity.Quiet, after(SESSION_DONE_LINGER_MILLIS + 1))
    }

    @Test
    fun aReadySessionThatNeverPrintedIsStarting() {
        assertEquals(SessionActivity.Starting, deriveSessionActivity(snapshot(), now))
    }

    @Test
    fun aSessionStillWithinItsStartPatienceIsStarting() {
        val justStarted = now - SESSION_START_PATIENCE_MILLIS + 1
        assertEquals(SessionActivity.Starting, deriveSessionActivity(snapshot(startedAtMillis = justStarted), now))
        assertEquals(
            SessionActivity.Starting,
            deriveSessionActivity(snapshot(status = TerminalSessionStatus.STARTING, startedAtMillis = justStarted), now),
        )
    }

    @Test
    fun aSessionThatNeverPrintedStopsStartingOnceItsPatienceRunsOut() {
        val longAgo = now - SESSION_START_PATIENCE_MILLIS
        val running: (String) -> Boolean? = { true }
        // A terminal kept from an earlier launch, not yet shown, never reports output.
        assertEquals(
            SessionActivity.Idle,
            deriveSessionActivity(snapshot(status = TerminalSessionStatus.STARTING, startedAtMillis = longAgo), now),
        )
        // Silence with nothing printed is not "needs you": that is reserved for an agent that spoke and stopped.
        assertEquals(
            SessionActivity.Idle,
            deriveSessionActivity(snapshot(startedAtMillis = longAgo), now, hasRunningProgram = running),
        )
    }

    @Test
    fun recentOutputMeansWorking() {
        val activity = deriveSessionActivity(snapshot(lastOutputAtMillis = now - 2_000), now)

        assertEquals(SessionActivity.Working, activity)
    }

    @Test
    fun outputExactlyAtTheWindowStillCountsAsWorking() {
        val activity = deriveSessionActivity(
            snapshot(lastOutputAtMillis = now - SESSION_WORKING_WINDOW_MILLIS),
            now,
        )

        assertEquals(SessionActivity.Working, activity)
    }

    @Test
    fun silenceBeyondTheWindowIsQuietNotWorking() {
        val activity = deriveSessionActivity(
            snapshot(lastOutputAtMillis = now - SESSION_WORKING_WINDOW_MILLIS - 1),
            now,
        )

        assertEquals(SessionActivity.Quiet, activity)
    }

    @Test
    fun aCleanExitIsExitedAndNeverDone() {
        val activity = deriveSessionActivity(
            snapshot(status = TerminalSessionStatus.EXITED, exitCode = 0),
            now,
        )

        assertEquals(SessionActivity.Exited, activity)
    }

    @Test
    fun anExitWithoutAKnownCodeIsExitedNotFailed() {
        val activity = deriveSessionActivity(snapshot(status = TerminalSessionStatus.EXITED), now)

        assertEquals(SessionActivity.Exited, activity)
    }

    @Test
    fun aNonZeroExitIsFailed() {
        val activity = deriveSessionActivity(
            snapshot(status = TerminalSessionStatus.EXITED, exitCode = 130),
            now,
        )

        assertEquals(SessionActivity.Failed, activity)
    }

    @Test
    fun aFailedStatusIsFailedEvenWithRecentOutput() {
        val activity = deriveSessionActivity(
            snapshot(status = TerminalSessionStatus.FAILED, lastOutputAtMillis = now),
            now,
        )

        assertEquals(SessionActivity.Failed, activity)
    }

    @Test
    fun aShellAtItsPromptIsIdleNotQuietHoweverLongItHasBeenSilent() {
        val activity = deriveSessionActivity(
            snapshot(lastOutputAtMillis = now - 10 * SESSION_WORKING_WINDOW_MILLIS),
            now,
            hasRunningProgram = { false },
        )

        assertEquals(SessionActivity.Idle, activity)
    }

    @Test
    fun silenceWhileAProgramRunsIsStillQuiet() {
        val activity = deriveSessionActivity(
            snapshot(lastOutputAtMillis = now - SESSION_WORKING_WINDOW_MILLIS - 1),
            now,
            hasRunningProgram = { true },
        )

        assertEquals(SessionActivity.Quiet, activity)
    }

    @Test
    fun aStartingStatusStaysStartingEvenWithOutput() {
        val activity = deriveSessionActivity(
            snapshot(status = TerminalSessionStatus.STARTING, lastOutputAtMillis = now),
            now,
        )

        assertEquals(SessionActivity.Starting, activity)
    }
}
