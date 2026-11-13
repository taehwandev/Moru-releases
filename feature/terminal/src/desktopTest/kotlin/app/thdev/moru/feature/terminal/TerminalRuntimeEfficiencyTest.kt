package app.thdev.moru.feature.terminal

import java.awt.GraphicsEnvironment
import org.junit.Assume.assumeFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerminalRuntimeEfficiencyTest {
    @Test
    fun anIdleNativePaneDoesNotPollEveryRenderFrame() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        TerminalEfficiencyWindow().use { fixture ->
            fixture.waitForSession(fixture.first)
            Thread.sleep(2_000)
            val before = fixture.revisionReads.get()
            Thread.sleep(2_000)
            val reads = fixture.revisionReads.get() - before
            assertTrue(reads <= 20, "An idle pane read its manager revision $reads times in two seconds")
        }
    }

    @Test
    fun boundedPollingStillShowsExternalSessionAddSelectionAndClose() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        TerminalEfficiencyWindow().use { fixture ->
            fixture.waitForSession(fixture.first)
            val second = fixture.addSession()
            fixture.waitForSession(second)
            fixture.waitUntil("new session selection") { fixture.keyboardRequests(second) > 0 }

            val requests = fixture.keyboardRequests(fixture.first)
            fixture.manager.selectSession(fixture.first)
            fixture.waitUntil("external selection") { fixture.keyboardRequests(fixture.first) > requests }
            assertEquals(fixture.first, fixture.manager.selectedSessionId())

            fixture.manager.closeSession(second)
            fixture.waitUntil("closed session removal") { !fixture.isShowing(second) }
            assertTrue(fixture.isShowing(fixture.first), "Closing another session must preserve the visible terminal")
            assertEquals(listOf(fixture.first), fixture.manager.listSessions().map { it.id })
        }
    }
}
