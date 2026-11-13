package app.thdev.moru.feature.terminal

import com.jediterm.terminal.model.SelectionUtil
import com.jediterm.terminal.ui.TerminalPanel
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/** Local clipboard gestures take precedence over a CLI's mouse reporting and terminal input. */
internal class TerminalClipboardActions(
    private val panel: TerminalPanel,
    private val copyToClipboard: (String) -> Unit,
) {
    private var copyingWithMouse = false
    private var shortcutKey: Int? = null

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val shortcut = shortcutKey
        if (event.id == KeyEvent.KEY_TYPED && shortcut != null && event.keyChar.uppercaseChar().code == shortcut) {
            event.consume()
            return true
        }
        if (event.id == KeyEvent.KEY_RELEASED && event.keyCode == shortcut) {
            shortcutKey = null
            event.consume()
            return true
        }
        if (event.id == KeyEvent.KEY_PRESSED) shortcutKey = null
        if (!event.isMetaDown || event.isControlDown || event.isAltDown || event.isShiftDown) return false
        val key = if (event.id == KeyEvent.KEY_TYPED) event.keyChar.uppercaseChar().code else event.keyCode
        if (key != KeyEvent.VK_C && key != KeyEvent.VK_A) return false
        if (event.id == KeyEvent.KEY_PRESSED) {
            shortcutKey = key
            if (key == KeyEvent.VK_A) {
                panel.selectAll()
                panel.repaint()
            } else copySelection()
        }
        // Local shortcuts must never type their trailing a/c into the CLI, including an empty selection.
        event.consume()
        return true
    }

    fun reset() {
        copyingWithMouse = false
        shortcutKey = null
    }

    fun handleMouseEvent(event: MouseEvent): Boolean {
        if (!SwingUtilities.isRightMouseButton(event) && !event.isPopupTrigger) return false
        if (event.id == MouseEvent.MOUSE_PRESSED) {
            copyingWithMouse = copySelection()
            if (copyingWithMouse) panel.requestFocusInWindow()
        } else if (event.id == MouseEvent.MOUSE_RELEASED && event.isPopupTrigger && !copyingWithMouse) {
            copyingWithMouse = copySelection()
        }
        if (!copyingWithMouse) return false
        event.consume()
        if (event.id == MouseEvent.MOUSE_CLICKED) copyingWithMouse = false
        return true
    }

    private fun copySelection(): Boolean {
        val selection = panel.selection ?: return false
        val buffer = panel.terminalTextBuffer
        val text = run {
            buffer.lock()
            // The selection's end cell is inclusive; getSelectionText wants the run JediTerm copies, ending after it.
            try { selection.pointsForRun(buffer.width).let { (start, end) -> SelectionUtil.getSelectionText(start, end, buffer) } }
            finally { buffer.unlock() }
        }.removeSuffix("\n") // A drag past the last line's end must not paste as a submitted line.
            .let { raw -> tidyCopiedTerminalText(raw, buffer.width) }
        if (text.isEmpty()) return false
        copyToClipboard(text)
        return true
    }

    companion object {
        /** JediTerm keeps the selection model; plain drags and word/line selection stay local in a TUI. */
        fun isSelectionGesture(event: MouseEvent): Boolean =
            SwingUtilities.isLeftMouseButton(event) &&
                (event.id == MouseEvent.MOUSE_DRAGGED ||
                    (event.id == MouseEvent.MOUSE_CLICKED && event.clickCount > 1))
    }
}
