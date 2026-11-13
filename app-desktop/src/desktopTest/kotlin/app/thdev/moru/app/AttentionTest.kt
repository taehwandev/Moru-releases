package app.thdev.moru.app

import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.core.terminal.SESSION_WORKING_WINDOW_MILLIS
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttentionTest {
    private val now = 1_000_000L
    private val titles = mapOf("t1" to "Fix login", "t2" to "Docs")
    private val sessionIds = mutableSetOf<String>()

    @AfterTest
    fun forgetObservedSessions() {
        sessionIds.forEach(SessionResponseAttention::forget)
    }

    private fun session(
        id: String,
        task: String?,
        command: String? = "claude",
        silentForMillis: Long? = 60_000,
        status: TerminalSessionStatus = TerminalSessionStatus.READY,
        exitCode: Int? = null,
        kind: TerminalSessionKind = TerminalSessionKind.TERMINAL,
    ) = TerminalSessionSnapshot(
        id = id,
        spec = TerminalSessionSpec(title = id, attachedItemId = task, startupCommand = command, kind = kind),
        status = status,
        lastOutputAtMillis = silentForMillis?.let { now - it },
        exitCode = exitCode,
    ).also { sessionIds += id }

    private fun candidates(vararg sessions: TerminalSessionSnapshot) = attentionCandidates(titles, sessions.toList(), now)

    @Test
    fun anAgentThatStoppedPrintingHasFinishedAtOnceAndOneStillPrintingHasNot() {
        assertEquals(listOf(AttentionCandidate("t1", "Fix login", AttentionKind.Finished, agent = "Claude", sessionId = "a")), candidates(session("a", "t1", silentForMillis = 9_000)))
        SessionResponseAttention.observe("a", SessionActivity.Working)
        assertEquals(emptyList(), candidates(session("a", "t1", silentForMillis = 5_000)))
    }

    @Test
    fun anAcknowledgedResponseDoesNotReappearInAlertsAfterDelayedOutput() {
        val finished = session("viewed-alert", "t1")
        assertEquals(AttentionKind.Finished, candidates(finished).single().kind)
        kotlin.test.assertTrue(SessionResponseAttention.acknowledge(finished.id))
        val redrawn = finished.copy(lastOutputAtMillis = now)
        assertTrue(candidates(redrawn).isEmpty())
        assertTrue(attentionCandidates(titles, listOf(redrawn), now + 9_000).isEmpty())
        SessionResponseAttention.observe(finished.id, SessionActivity.Working)
        assertEquals(AttentionKind.Finished, attentionCandidates(titles, listOf(redrawn), now + 9_000).single().kind)
    }

    @Test
    fun aFailedTerminalAlwaysNeedsTheUserEvenARecentOne() {
        val failed = session("b", "t1", command = "npm test", silentForMillis = 1_000, status = TerminalSessionStatus.EXITED, exitCode = 1)

        assertEquals(listOf(AttentionCandidate("t1", "Fix login", AttentionKind.Failed, sessionId = "b")), candidates(failed))
    }

    @Test
    fun aPlainTerminalSittingAtItsPromptIsIdleNotWaiting() {
        assertEquals(emptyList(), candidates(session("p", "t1", command = null, silentForMillis = 600_000)))
        assertEquals(emptyList(), candidates(session("p", "t1", command = "npm run dev", silentForMillis = 600_000)))
    }

    @Test
    fun aQuietAgentNeedsTheUserEvenWhileAnotherAgentInTheTaskStillWorks() {
        assertEquals(
            listOf(AttentionCandidate("t1", "Fix login", AttentionKind.Finished, agent = "Claude", sessionId = "a")),
            candidates(session("a", "t1", silentForMillis = 60_000), session("w", "t1", command = "codex", silentForMillis = 2_000)),
        )
    }

    @Test
    fun theAgentSilentLongestIsTheOneNamed() {
        assertEquals(
            listOf(AttentionCandidate("t1", "Fix login", AttentionKind.Finished, agent = "Codex", sessionId = "c")),
            candidates(session("a", "t1", silentForMillis = 50_000), session("c", "t1", command = "codex", silentForMillis = 90_000)),
        )
    }

    @Test
    fun aCleanExitIsNotAFailureAndTasksWithoutATitleOrOtherKindsAreIgnored() {
        assertEquals(emptyList(), candidates(session("c", "t1", status = TerminalSessionStatus.EXITED, exitCode = 0, silentForMillis = 1_000)))
        assertEquals(emptyList(), candidates(session("x", "unknown-task", silentForMillis = 60_000)))
        assertEquals(emptyList(), candidates(session("n", null, silentForMillis = 60_000)))
        assertEquals(emptyList(), candidates(session("v", "t1", silentForMillis = 60_000, kind = TerminalSessionKind.WEB_PREVIEW)))
    }

    @Test
    fun eachTaskIsListedOnceWithItsOwnReason() {
        val result = candidates(
            session("a", "t1", silentForMillis = 60_000),
            session("f", "t2", command = "make", silentForMillis = 1_000, status = TerminalSessionStatus.EXITED, exitCode = 2),
        )

        assertEquals(setOf("t1" to AttentionKind.Finished, "t2" to AttentionKind.Failed), result.map { it.taskId to it.kind }.toSet())
    }

    @Test
    fun anAgentAskingAQuestionNeedsTheUserAtOnceEvenWhileItStillRedraws() {
        val questions = mapOf("a" to "Do you want to proceed?")
        val asking = attentionCandidates(titles, listOf(session("a", "t1", silentForMillis = 1_000)), now, questionOf = questions::get)

        assertEquals(
            listOf(AttentionCandidate("t1", "Fix login", AttentionKind.Asking, agent = "Claude", question = "Do you want to proceed?", sessionId = "a")),
            asking,
        )
    }

    @Test
    fun aFailureStillOutranksAQuestionAndAQuestionOutranksSilence() {
        val questions = mapOf("c" to "Would you like to run the following command?")
        val failed = session("f", "t1", command = "make", silentForMillis = 1_000, status = TerminalSessionStatus.EXITED, exitCode = 2)
        val asking = session("c", "t1", command = "codex", silentForMillis = 1_000)
        val quiet = session("a", "t1", silentForMillis = 90_000)

        assertEquals(AttentionKind.Failed, attentionCandidates(titles, listOf(failed, asking), now, questionOf = questions::get).single().kind)
        val result = attentionCandidates(titles, listOf(quiet, asking), now, questionOf = questions::get).single()
        assertEquals(AttentionKind.Asking to "Codex", result.kind to result.agent)
    }

    private val quiet = AttentionCandidate("t1", "Fix login", AttentionKind.Finished)

    @Test
    fun aWaitingQuestionIsNotAnnouncedAgainWhenItsScreenBecomesQuietAndRedraws() {
        val policy = AttentionPolicy()
        val asking = quiet.copy(kind = AttentionKind.Asking, question = "Proceed?")

        assertEquals(listOf(asking), policy.update(listOf(asking), null, false))
        repeat(3) {
            assertEquals(emptyList(), policy.update(listOf(quiet), null, false))
            assertEquals(emptyList(), policy.update(listOf(asking), null, false))
        }
    }

    @Test
    fun aStretchOfNeedingAttentionIsAnnouncedOnceAndAgainOnlyAfterItEnded() {
        var clock = now
        val policy = AttentionPolicy { clock }

        assertEquals(listOf(quiet), policy.update(listOf(quiet), viewedTaskId = null, windowFocused = false))
        assertEquals(emptyList(), policy.update(listOf(quiet), viewedTaskId = null, windowFocused = false))
        assertEquals(emptyList(), policy.update(emptyList(), viewedTaskId = null, windowFocused = false))
        clock += SESSION_WORKING_WINDOW_MILLIS
        assertEquals(listOf(quiet), policy.update(listOf(quiet), viewedTaskId = null, windowFocused = false))
    }

    @Test
    fun aWaitingPromptMissingBrieflyDuringRepeatedRedrawsKeepsItsAnnouncement() {
        var clock = now
        val policy = AttentionPolicy { clock }
        val asking = quiet.copy(kind = AttentionKind.Asking, question = "Proceed?")

        assertEquals(listOf(asking), policy.update(listOf(asking), null, false))
        repeat(6) {
            clock += 2_000
            assertEquals(emptyList(), policy.update(emptyList(), null, false))
            clock += 2_000
            assertEquals(emptyList(), policy.update(listOf(asking), null, false))
        }
    }

    @Test
    fun eachTaskEndsItsEpisodeIndependentlyAndNewAttentionIsStillAnnounced() {
        var clock = now
        val policy = AttentionPolicy { clock }
        val other = AttentionCandidate("t2", "Docs", AttentionKind.Asking, question = "Proceed?")

        assertEquals(listOf(quiet, other), policy.update(listOf(quiet, other), null, false))
        assertEquals(emptyList(), policy.update(listOf(other), null, false))
        clock += SESSION_WORKING_WINDOW_MILLIS
        assertEquals(emptyList(), policy.update(listOf(other), null, false))
        assertEquals(listOf(quiet), policy.update(listOf(quiet, other), null, false))
    }

    @Test
    fun aQuestionAfterSilenceIsNewButRepeatedReasonChangesDoNotReplayIt() {
        val policy = AttentionPolicy()
        val asking = quiet.copy(kind = AttentionKind.Asking, question = "Proceed?")

        assertEquals(listOf(quiet), policy.update(listOf(quiet), null, false))
        assertEquals(listOf(asking), policy.update(listOf(asking), null, false))
        assertEquals(emptyList(), policy.update(listOf(quiet), null, false))
        assertEquals(emptyList(), policy.update(listOf(asking), null, false))
    }

    @Test
    fun theTaskTheUserIsLookingAtInAFocusedWindowIsNeverAnnouncedButAnotherOneIs() {
        val policy = AttentionPolicy()
        val other = AttentionCandidate("t2", "Docs", AttentionKind.Failed)

        assertEquals(listOf(other), policy.update(listOf(quiet, other), viewedTaskId = "t1", windowFocused = true))
        // Looking at it later does not make it a new stretch, so nothing is announced after the window loses focus.
        assertEquals(emptyList(), policy.update(listOf(quiet, other), viewedTaskId = "t1", windowFocused = false))
    }

    @Test
    fun aQuietAndAFailureOfTheSameTaskAreSeparateStretches() {
        val policy = AttentionPolicy()
        val failed = AttentionCandidate("t1", "Fix login", AttentionKind.Failed)

        policy.update(listOf(quiet), null, false)

        assertEquals(listOf(failed), policy.update(listOf(failed), null, false))
    }
}
