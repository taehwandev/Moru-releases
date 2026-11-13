package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionThemeColorsTest {
    @Test
    fun changingThemeMarksOnlyTheSessionThatHasReadAnOlderBackground() {
        val session = "theme-observed"
        val other = "theme-unqueried"
        try {
            SessionThemeColors.recordCanvas(session, 0x1a1b26)
            SessionThemeColors.recordCanvas(other, 0x1a1b26)
            SessionThemeColors.recordBackgroundColor(session, 0x1a1b26)
            SessionThemeColors.recordCanvas(session, 0xfafbfc)
            SessionThemeColors.recordCanvas(other, 0xfafbfc)
            assertTrue(SessionThemeColors.hasStaleBackground(session))
            assertFalse(SessionThemeColors.hasStaleBackground(other))

            // Going back to the colors the program read needs no restart.
            SessionThemeColors.recordCanvas(session, 0x1a1b26)
            assertFalse(SessionThemeColors.hasStaleBackground(session))
            SessionThemeColors.recordCanvas(session, 0xfafbfc)
            assertTrue(SessionThemeColors.hasStaleBackground(session))

            // A restarted or theme-aware program asks again; its observation is now current.
            SessionThemeColors.recordBackgroundColor(session, 0xfafbfc)
            assertFalse(SessionThemeColors.hasStaleBackground(session))
        } finally {
            SessionThemeColors.forget(session)
            SessionThemeColors.forget(other)
        }
        assertFalse(SessionThemeColors.hasStaleBackground(session))
    }
}
