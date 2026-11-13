package app.thdev.moru.core.terminal

import app.thdev.moru.core.model.session.SessionActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Opening a finished agent's session from its notification attaches, resizes and focuses its terminal, and the agent
 * redraws in answer. That redraw used to count as the agent working, so the session flipped to running and then to
 * finished again, announcing itself in a loop.
 */
class SessionRedrawActivityTest {
    @Test
    fun aLateRedrawKeepsTheViewedResponseWaitingButSubmittedInputRearmsTheNextResponse() {
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false)
        val session = manager.createSession(TerminalSessionSpec())
        val id = session.id
        fun activity(at: Long): SessionActivity {
            val snapshot = manager.listSessions().single { it.id == id }
            return SessionResponseAttention.project(id, deriveSessionActivity(snapshot, at,
                hasRunningProgram = { true }, asksUser = { false }, showsBusy = { false }))
        }
        try {
            manager.reportOutput(id, 10_000)
            assertEquals(SessionActivity.Quiet, activity(20_000))
            assertTrue(SessionResponseAttention.acknowledge(id))
            assertEquals(SessionActivity.Waiting, activity(20_000))

            SessionStimuli.record(id, 50_000)
            manager.reportOutput(id, 52_500)
            assertEquals(SessionActivity.Waiting, activity(52_500))
            assertEquals(SessionActivity.Waiting, activity(61_000))

            SessionStimuli.record(id, 70_000)
            SessionResponseAttention.inputSubmitted(id)
            assertEquals(SessionActivity.Waiting, activity(70_000), "submission alone cannot invent a new completion")
            manager.reportOutput(id, 70_100)
            assertEquals(70_100L, manager.listSessions().single().lastOutputAtMillis,
                "a fast response to submitted input is not a passive redraw")
            assertEquals(SessionActivity.Working, activity(70_100))
            assertEquals(SessionActivity.Quiet, activity(79_000))
            assertTrue(SessionResponseAttention.acknowledge(id))
            assertEquals(SessionActivity.Waiting, activity(79_000))

            SessionResponseAttention.inputSubmitted(id)
            manager.closeSession(id)
            assertEquals(SessionActivity.Quiet, SessionResponseAttention.project(id, SessionActivity.Quiet))
        } finally {
            manager.closeSession(id)
            SessionResponseAttention.forget(id)
        }
    }

    @Test
    fun outputJustAfterMoruProddedTheSessionIsNotWorkButStillProvesItStarted() {
        val stimuli = mutableMapOf<String, Long>()
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false) { id, at ->
            stimuli[id]?.let { at - it in 0..SessionStimuli.ReactionWindowMillis } ?: false
        }
        val session = manager.createSession(TerminalSessionSpec(workingDirectory = System.getProperty("user.dir")))

        manager.reportOutput(session.id, atMillis = 10_000)
        stimuli[session.id] = 50_000
        manager.reportOutput(session.id, atMillis = 50_400)
        manager.reportOutput(session.id, atMillis = 51_900)

        val snapshot = manager.listSessions().single { it.id == session.id }
        assertEquals(10_000L, snapshot.lastOutputAtMillis, "the redraw after a resize must not read as work")
        assertEquals(TerminalSessionStatus.READY, snapshot.status)

        manager.reportOutput(session.id, atMillis = 53_000)
        assertEquals(53_000L, manager.listSessions().single { it.id == session.id }.lastOutputAtMillis)
    }

    @Test
    fun aRedrawOnlySessionStillBecomesReady() {
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false) { _, _ -> true }
        val session = manager.createSession(TerminalSessionSpec(workingDirectory = System.getProperty("user.dir")))

        manager.reportOutput(session.id, atMillis = 1_000)

        val snapshot = manager.listSessions().single { it.id == session.id }
        assertEquals(TerminalSessionStatus.READY, snapshot.status)
        assertEquals(null, snapshot.lastOutputAtMillis)
    }

    @Test
    fun theReactionWindowStartsAtTheStimulusAndEnds() {
        SessionStimuli.record("s", 1_000)
        assertTrue(SessionStimuli.isReaction("s", 1_000))
        assertTrue(SessionStimuli.isReaction("s", 3_000))
        assertFalse(SessionStimuli.isReaction("s", 3_001))
        assertFalse(SessionStimuli.isReaction("s", 999))
        assertFalse(SessionStimuli.isReaction("other", 1_500))
        SessionStimuli.forget("s")
        assertFalse(SessionStimuli.isReaction("s", 1_500))
    }

    @Test
    fun aRealTerminalReportsResizeAndInputAsStimuli() {
        val stimuli = AtomicInteger()
        val started = CountDownLatch(1)
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
            onOutput = { started.countDown() },
            onStimulus = { stimuli.incrementAndGet() },
        )
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (connector.isConnected) {
                if (connector.read(buffer, 0, buffer.size) < 0) break
            }
        }
        connector.write("echo hi\n")
        assertEquals(1, stimuli.get())
        connector.resize(com.jediterm.core.util.TermSize(100, 30))
        assertEquals(2, stimuli.get())
        assertTrue(started.await(10, TimeUnit.SECONDS))
        connector.close()
        reader.join(2_000)
    }

    @Test
    fun aRealConnectorSignalsSubmissionBeforeItsFirstResponseAndIgnoresFocusAndTyping() {
        val submissions = AtomicInteger()
        val response = CountDownLatch(1)
        val submittedBeforeResponse = java.util.concurrent.atomic.AtomicBoolean(false)
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
            onInputSubmitted = { submissions.incrementAndGet() },
            onOutput = {
                if (submissions.get() > 0) {
                    submittedBeforeResponse.set(true)
                    response.countDown()
                }
            },
        )
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (connector.isConnected) {
                if (connector.read(buffer, 0, buffer.size) < 0) break
            }
        }
        try {
            connector.resize(com.jediterm.core.util.TermSize(100, 30))
            connector.write("printf response")
            assertEquals(0, submissions.get())
            connector.write("\r")
            assertEquals(1, submissions.get())
            assertTrue(response.await(10, TimeUnit.SECONDS))
            assertTrue(submittedBeforeResponse.get())
        } finally {
            connector.close()
            reader.join(2_000)
        }
    }
}
