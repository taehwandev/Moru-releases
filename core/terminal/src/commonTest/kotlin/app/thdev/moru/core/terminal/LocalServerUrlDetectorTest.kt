package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalServerUrlDetectorTest {
    @Test
    fun findsTheLocalAddressAViteServerPrints() {
        val url = LocalServerUrlDetector().accept("  ➜  Local:   http://localhost:5173/\n  ➜  Network: use --host\n")

        assertEquals("http://localhost:5173/", url)
    }

    @Test
    fun ignoresColorCodesAroundTheAddress() {
        val url = LocalServerUrlDetector().accept("Local: \u001B[36mhttp://127.0.0.1:3000\u001B[0m/\n")

        assertEquals("http://127.0.0.1:3000/", url)
    }

    @Test
    fun rewritesAnUnspecifiedAddressToLocalhost() {
        assertEquals("http://localhost:8080", LocalServerUrlDetector().accept("Listening on http://0.0.0.0:8080\n"))
    }

    @Test
    fun neverReturnsAnAddressOutsideThisMachine() {
        val detector = LocalServerUrlDetector()

        assertNull(detector.accept("Network: http://192.168.0.5:5173/\n"))
        assertNull(detector.accept("docs at https://example.com/localhost:3000\n"))
    }

    @Test
    fun joinsAnAddressSplitAcrossTwoChunks() {
        val detector = LocalServerUrlDetector()

        assertNull(detector.accept("Local: http://local"))
        assertEquals("http://localhost:4000/", detector.accept("host:4000/\n"))
    }

    @Test
    fun dropsPunctuationThatEndsASentence() {
        assertEquals("http://localhost:9000", LocalServerUrlDetector().accept("Open http://localhost:9000.\n"))
    }

    @Test
    fun keepsTheLastAddressWhenSeveralArePrinted() {
        val url = LocalServerUrlDetector().accept("http://localhost:3000\nrestarted on http://localhost:3001\n")

        assertEquals("http://localhost:3001", url)
    }

    @Test
    fun anAddressStillBeingTypedIsNotReportedUntilItEnds() {
        val detector = LocalServerUrlDetector()

        assertNull(detector.accept("ready on http://localhost:30"))
        assertEquals("http://localhost:3000", detector.accept("00\n"))
    }

    @Test
    fun sessionsRememberTheirOwnNewestAddress() {
        LocalServerUrls.record("a", "http://localhost:1")
        LocalServerUrls.record("a", "http://localhost:2")
        LocalServerUrls.record("b", "http://localhost:3")

        assertEquals("http://localhost:2", LocalServerUrls.urlFor("a"))
        assertEquals("http://localhost:3", LocalServerUrls.urlFor("b"))
        LocalServerUrls.forget("a")
        assertNull(LocalServerUrls.urlFor("a"))
    }
}
