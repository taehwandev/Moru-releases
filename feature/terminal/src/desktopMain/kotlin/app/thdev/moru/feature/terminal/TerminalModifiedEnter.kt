package app.thdev.moru.feature.terminal

import java.awt.event.InputEvent
import java.awt.event.KeyEvent

/** Preserves Shift+Enter as CSI-u without changing JediTerm's other key bindings. */
internal class TerminalModifiedEnter(private val send: (String) -> Unit) {
    private var awaitingTypedReturn = false
    private var awaitingRelease = false

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.isConsumed) return false
        val handled = when (event.id) {
            KeyEvent.KEY_PRESSED -> {
                reset()
                if (event.keyCode != KeyEvent.VK_ENTER || event.modifiersEx != InputEvent.SHIFT_DOWN_MASK) return false
                // CSI-u encodes Enter as 13 and Shift as 1 + its modifier bit (2).
                send("\u001B[13;2u")
                awaitingTypedReturn = true
                awaitingRelease = true
                true
            }
            KeyEvent.KEY_TYPED -> {
                val returnEvent = event.keyChar == '\n' || event.keyChar == '\r'
                val suppress = awaitingTypedReturn && returnEvent
                awaitingTypedReturn = false
                suppress
            }
            KeyEvent.KEY_RELEASED -> {
                if (!awaitingRelease || event.keyCode != KeyEvent.VK_ENTER) return false
                reset()
                true
            }
            else -> false
        }
        if (handled) event.consume()
        return handled
    }

    fun reset() {
        awaitingTypedReturn = false
        awaitingRelease = false
    }
}
