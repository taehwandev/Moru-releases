package app.thdev.moru.app

import androidx.compose.ui.input.key.nativeKeyCode
import java.awt.Component
import java.awt.KeyEventDispatcher
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

/** Enabled app commands run before an embedded Swing view can consume their keys. */
internal class AppShortcutDispatcher(
    private val entries: () -> List<AppMenuEntry>,
    private val macOs: Boolean,
    private val belongsToWindow: (Component) -> Boolean,
) : KeyEventDispatcher {
    private val pressed = mutableSetOf<Int>()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.isConsumed || !belongsToWindow(event.component)) return false
        if (event.id == KeyEvent.KEY_RELEASED && pressed.remove(event.keyCode)) return consume(event)
        if (event.id == KeyEvent.KEY_TYPED && pressed.isNotEmpty()) return consume(event)
        if (event.id != KeyEvent.KEY_PRESSED) return false
        if (if (macOs) !event.isMetaDown else !event.isControlDown) {
            reset()
            return false
        }
        if (event.keyCode in pressed) return consume(event)
        val modifier = if (macOs) InputEvent.META_DOWN_MASK else InputEvent.CTRL_DOWN_MASK
        val entry = entries().firstOrNull {
            val extra = (if (it.shift) InputEvent.SHIFT_DOWN_MASK else 0) or (if (it.alt) InputEvent.ALT_DOWN_MASK else 0)
            it.enabled && it.key?.nativeKeyCode == event.keyCode && (event.modifiersEx and ModifierMask) == (modifier or extra)
        } ?: return false
        pressed += event.keyCode
        event.consume()
        entry.onClick()
        return true
    }

    fun reset() = pressed.clear()

    private fun consume(event: KeyEvent): Boolean {
        event.consume()
        return true
    }

    private companion object {
        const val ModifierMask = InputEvent.META_DOWN_MASK or InputEvent.CTRL_DOWN_MASK or
            InputEvent.ALT_DOWN_MASK or InputEvent.ALT_GRAPH_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
    }
}
