package app.thdev.moru.core.terminal

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionProgramObservationsTest {
    @Test
    fun manyViewReadsShareOneBackgroundObservationAndTheNextTickSeesAnExit() {
        val shells = mutableMapOf("session" to 10L)
        var running = true
        var queries = 0
        val observations = SessionProgramObservations({ shells.toMap() }, shells::get) { queries++; running }
        assertNull(observations.readingOf("session"))
        assertEquals(0, queries, "First composition must not query the operating system")
        observations.refresh()
        repeat(10_000) { assertEquals(true, observations.readingOf("session")) }
        assertEquals(1, queries, "Rows, task activity and attention must share the same observation")
        running = false
        observations.refresh()
        assertEquals(false, observations.readingOf("session"))
        assertEquals(2, queries)
    }

    @Test
    fun aSlowProcessScanDoesNotHoldUpAViewReadingAndCannotPublishForAReplacedShell() {
        val shells = java.util.concurrent.ConcurrentHashMap(mapOf("session" to 10L))
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        var slow = false
        val observations = SessionProgramObservations({ shells.toMap() }, shells::get) {
            if (slow) { entered.countDown(); check(release.await(5, TimeUnit.SECONDS)) }
            true
        }
        observations.refresh()
        slow = true
        val scanner = thread(isDaemon = true) { observations.refresh() }
        val reader = Executors.newSingleThreadExecutor()
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertEquals(true, reader.submit<Boolean?> { observations.readingOf("session") }.get(1, TimeUnit.SECONDS))
            shells["session"] = 11L
            assertNull(observations.readingOf("session"))
            release.countDown()
            scanner.join(5_000)
            assertTrue(!scanner.isAlive)
            assertNull(observations.readingOf("session"), "Late results from the old PID must stay unknown")
        } finally {
            release.countDown()
            scanner.join(5_000)
            reader.shutdownNow()
        }
    }

    @Test
    fun forgottenAndUnavailableSessionsRemainUnknownAndRecoverOnTheNextReading() {
        val shells = mutableMapOf("session" to 10L)
        var available = false
        val observations = SessionProgramObservations({ shells.toMap() }, shells::get) {
            if (!available) error("Process observation unavailable") else false
        }
        observations.refresh()
        assertNull(observations.readingOf("session"))
        assertNull(observations.readingOf("unknown"))
        available = true
        observations.refresh()
        assertEquals(false, observations.readingOf("session"))
        shells.clear()
        assertNull(observations.readingOf("session"))
        observations.refresh()
        assertNull(observations.readingOf("session"))
    }
}
