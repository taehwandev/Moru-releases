package app.thdev.moru.core.terminal

import app.thdev.moru.core.terminal.host.OutputRing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeChangeReportTest {
    @Test
    fun aProgramThatAskedForThemeNotificationsIsToldDarkOrLight() {
        val report = ThemeChangeReport()
        report.accept("\u001B[?1004h\u001B[?20")
        report.accept("31h")

        assertEquals("\u001B[?997;2n", report.sequenceFor(dark = false))
        assertEquals("\u001B[?997;1n", report.sequenceFor(dark = true))
    }

    @Test
    fun aProgramThatAskedForNothingOrTurnedItOffGetsNothing() {
        val report = ThemeChangeReport()
        report.accept("\u001B[?2004;1004h")
        assertNull(report.sequenceFor(dark = false))

        report.accept("\u001B[?2031h\u001B[?2031l")
        assertNull(report.sequenceFor(dark = false))
    }

    @Test
    fun aReopenedTerminalStillKnowsTheProgramAskedForThemeNotifications() {
        val ring = OutputRing(16)
        ring.append("\u001B[?2031h".toByteArray())
        ring.append("a long line that pushes the request out of the ring\n".toByteArray())

        assertTrue(String(ring.replay()).contains("\u001B[?2031h"))
    }
}
