package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionTitlesTest {
    @Test
    fun agentSummaryLosesItsSpinnerGlyph() {
        assertEquals("Fix CI checks detail link", agentTitleText("✳ Fix CI checks detail link"))
        assertEquals("Apple Silicon arm64 DMG만 배포", agentTitleText("π ⠸ Apple Silicon arm64 DMG만 배포"))
    }

    @Test
    fun genericTitlesSayNothing() {
        assertNull(agentTitleText("✳ Claude Code"))
        assertNull(agentTitleText("codex"))
        assertNull(agentTitleText("agy"))
        assertNull(agentTitleText("antigravity"))
        assertNull(agentTitleText("~/git/Moru"))
        assertNull(agentTitleText("user@mac: ~/git/Moru"))
        assertNull(agentTitleText("  ⠸ "))
    }

    @Test
    fun aGenericTitleClearsTheOldSummary() {
        SessionTitles.record("s", "✳ Reading the sidebar")
        assertEquals("Reading the sidebar", SessionTitles.titleFor("s"))
        SessionTitles.record("s", "zsh")
        assertNull(SessionTitles.titleFor("s"))
        SessionTitles.forget("s")
    }
}
