package app.thdev.moru.app

import kotlin.test.Test
import kotlin.test.assertEquals

class ShortcutTargetTest {
    @Test
    fun aTerminalClickedInTheTaskBesideTakesTheShortcutsThere() {
        assertEquals(ShortcutTarget("b", "b-2"), ShortcutTarget.of("a", besideShown = "b", activeTerminal = "b" to "b-2"))
    }

    @Test
    fun theSelectedTasksOwnTerminalKeepsThemThere() {
        assertEquals(ShortcutTarget("a", "a-1"), ShortcutTarget.of("a", besideShown = "b", activeTerminal = "a" to "a-1"))
        assertEquals(ShortcutTarget("a", null), ShortcutTarget.of("a", besideShown = "b", activeTerminal = null))
    }

    @Test
    fun aTaskNoLongerBesideFallsBackToTheSelectedTask() {
        assertEquals(ShortcutTarget("a", null), ShortcutTarget.of("a", besideShown = null, activeTerminal = "b" to "b-2"))
        assertEquals(ShortcutTarget("c", null), ShortcutTarget.of("c", besideShown = "d", activeTerminal = "b" to "b-2"))
    }
}
