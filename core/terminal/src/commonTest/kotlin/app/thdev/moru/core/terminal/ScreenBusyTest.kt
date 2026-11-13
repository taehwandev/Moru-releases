package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenBusyTest {
    @Test
    fun claudeAndCodexStatusLinesAtTheBottomMeanAtWork() {
        assertTrue(screenBusy(listOf("> fix the login", "", "✻ Thinking… (12s · ↑ 1.2k tokens · esc to interrupt)", "", "> ")))
        assertTrue(screenBusy(listOf("• Running gradle test", "", "▌ Working (48s • Esc to interrupt)", "", "› ")))
    }

    @Test
    fun aFinishedTurnOrOneScrolledFarUpIsNotAtWork() {
        assertFalse(screenBusy(listOf("Done. All tests pass.", "", "> ")))
        val scrolledAway = listOf("✻ Thinking… (esc to interrupt)") + List(10) { "line $it" }
        assertFalse(screenBusy(scrolledAway))
    }
}
