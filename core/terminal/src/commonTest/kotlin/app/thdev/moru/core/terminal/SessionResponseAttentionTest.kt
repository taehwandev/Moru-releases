package app.thdev.moru.core.terminal

import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionResponseAttentionTest {
    @Test
    fun acknowledgmentImmediatelyChangesOnlyThatSessionsRepeatedCompletionSamples() {
        val registry = SessionResponseAttentionRegistry()
        assertEquals(SessionActivity.Quiet, registry.observe("one", SessionActivity.Quiet))
        registry.observe("two", SessionActivity.Quiet)
        val before = registry.revision.value
        assertTrue(registry.acknowledge("one"))
        assertTrue(registry.revision.value > before)
        repeat(3) { assertEquals(SessionActivity.Waiting, registry.observe("one", SessionActivity.Quiet)) }
        assertEquals(SessionActivity.Quiet, registry.observe("two", SessionActivity.Quiet))
        assertFalse(registry.acknowledge("one"))
    }

    @Test
    fun seenResponsesSurviveARestartUntilANewTurn() {
        val before = SessionResponseAttentionRegistry()
        before.observe("seen", SessionActivity.Quiet)
        before.observe("unseen", SessionActivity.Quiet)
        assertTrue(before.acknowledge("seen"))
        assertEquals(setOf("seen"), before.acknowledged())

        // After a restart the retained terminal redraws (a quiet sample, or a projected redraw): still seen.
        val after = SessionResponseAttentionRegistry()
        after.restore(before.acknowledged())
        assertEquals(SessionActivity.Idle, after.observe("seen", SessionActivity.Idle))
        assertEquals(SessionActivity.Waiting, after.observe("seen", SessionActivity.Working, rearm = false))
        assertEquals(SessionActivity.Waiting, after.observe("seen", SessionActivity.Quiet))
        assertEquals(SessionActivity.Quiet, after.observe("unseen", SessionActivity.Quiet))

        // A real new turn drops it, and that change is announced so it is saved.
        val revision = after.revision.value
        assertEquals(SessionActivity.Working, after.observe("seen", SessionActivity.Working))
        assertTrue(after.revision.value > revision)
        assertEquals(emptySet(), after.acknowledged())
        assertEquals(SessionActivity.Quiet, after.observe("seen", SessionActivity.Quiet))
    }

    @Test
    fun aFreshWorkingOrStartingTurnRearmsWithoutDependingOnOutputTimestamp() {
        val registry = SessionResponseAttentionRegistry()
        for (working in listOf(SessionActivity.Working, SessionActivity.Starting)) {
            registry.observe("one", SessionActivity.Quiet)
            assertTrue(registry.acknowledge("one"))
            assertEquals(working, registry.observe("one", working))
            assertFalse(registry.acknowledge("one"))
            assertEquals(SessionActivity.Quiet, registry.observe("one", SessionActivity.Quiet))
        }
    }

    @Test
    fun permissionFailureAndExitRemainAuthoritativeAfterAcknowledgment() {
        val registry = SessionResponseAttentionRegistry()
        registry.observe("one", SessionActivity.Quiet)
        registry.acknowledge("one")
        for (activity in listOf(SessionActivity.Asking, SessionActivity.Failed, SessionActivity.Exited)) {
            assertEquals(activity, registry.observe("one", activity))
            assertFalse(registry.acknowledge("one"))
        }
    }

    @Test
    fun livePermissionPreventsAcknowledgingAStaleQuietRow() {
        val id = "response-permission"
        try {
            SessionResponseAttention.observe(id, SessionActivity.Quiet)
            SessionPermissions.observe(id, SessionPermissionPrompt("Allow?", "", emptyList()))
            assertFalse(SessionResponseAttention.acknowledge(id))
            SessionPermissions.forget(id)
            assertEquals(SessionActivity.Quiet, SessionResponseAttention.observe(id, SessionActivity.Quiet))
            assertTrue(SessionResponseAttention.acknowledge(id))
        } finally {
            SessionPermissions.forget(id)
            SessionResponseAttention.forget(id)
        }
    }

    @Test
    fun theLatestReportedActivityRejectsAStaleQuietProjection() {
        val id = "response-stale-projection"
        try {
            for (activity in listOf(SessionActivity.Working, SessionActivity.Asking, SessionActivity.Failed)) {
                SessionReportedActivities.record(id, activity)
                // A UI sample captured before the latest provider event may arrive after it.
                SessionResponseAttention.observe(id, SessionActivity.Quiet)
                assertFalse(SessionResponseAttention.acknowledge(id))
            }
        } finally {
            SessionReportedActivities.forget(id)
            SessionResponseAttention.forget(id)
        }
    }

    @Test
    fun reportedWorkAndBusyScreensRearmEvenWhenTheUiMissedTheWorkingSample() {
        val id = "response-between-samples"
        try {
            SessionResponseAttention.observe(id, SessionActivity.Quiet)
            SessionResponseAttention.acknowledge(id)
            SessionReportedActivities.record(id, SessionActivity.Working)
            SessionReportedActivities.record(id, SessionActivity.Quiet)
            assertEquals(SessionActivity.Quiet, SessionResponseAttention.observe(id, SessionActivity.Quiet))
            SessionResponseAttention.acknowledge(id)
            SessionBusyScreens.record(id, true)
            assertFalse(SessionResponseAttention.acknowledge(id))
            SessionBusyScreens.record(id, false)
            assertEquals(SessionActivity.Quiet, SessionResponseAttention.observe(id, SessionActivity.Quiet))
        } finally {
            SessionReportedActivities.forget(id)
            SessionBusyScreens.forget(id)
            SessionResponseAttention.forget(id)
        }
    }

    @Test
    fun closingASessionForgetsItsAcknowledgmentWithoutChangingOtherSessions() {
        val sessions = InMemoryTerminalSessionManager()
        val closed = sessions.createSession(TerminalSessionSpec())
        val kept = sessions.createSession(TerminalSessionSpec())
        try {
            for (session in listOf(closed, kept)) {
                SessionResponseAttention.observe(session.id, SessionActivity.Quiet)
                SessionResponseAttention.acknowledge(session.id)
            }
            sessions.closeSession(closed.id)
            assertFalse(SessionResponseAttention.acknowledge(closed.id))
            assertEquals(SessionActivity.Quiet, SessionResponseAttention.observe(closed.id, SessionActivity.Quiet))
            assertEquals(SessionActivity.Waiting, SessionResponseAttention.observe(kept.id, SessionActivity.Quiet))
        } finally {
            sessions.closeSession(kept.id)
            SessionResponseAttention.forget(closed.id)
        }
    }
}
