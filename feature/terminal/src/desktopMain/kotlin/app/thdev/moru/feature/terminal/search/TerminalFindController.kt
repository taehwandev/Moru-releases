package app.thdev.moru.feature.terminal.search

import com.jediterm.terminal.model.TerminalModelListener
import com.jediterm.terminal.model.TerminalSelection
import com.jediterm.terminal.model.TerminalTextBuffer
import com.jediterm.terminal.ui.TerminalPanel
import java.awt.BorderLayout
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.KeyStroke

/** Keeps native find local to one retained terminal, including while output changes its screen. */
internal class TerminalFindController(
    private val host: JPanel,
    private val panel: TerminalPanel,
    private val buffer: TerminalTextBuffer,
    private val restoreSelection: (TerminalSelection?) -> Unit,
    private val findShortcuts: List<KeyStroke>,
) {
    val bar = TerminalFindBar(::queryChanged, ::navigate, ::close, ::escape)
    var isOpen = false
        private set
    private val dirty = AtomicBoolean()
    private var changedQuery = false
    private var previousSelection: TerminalSelection? = null
    private var searchSelection: TerminalSelection? = null
    private var consumedKey = KeyEvent.VK_UNDEFINED
    private val listener = TerminalModelListener { dirty.set(true) }
    // Coalesce process output and typing. A closed finder has no listener or timer work.
    private val timer = Timer(150) { if (host.isShowing && dirty.get() && bar.queryField.text.isNotEmpty()) refreshNow() }

    val keyboardTarget: JComponent get() = if (isOpen) bar.queryField else panel

    /** Consume the whole native key sequence even if opening or closing transfers the focus halfway through it. */
    fun handleTerminalKey(event: KeyEvent): Boolean {
        val handled = when (event.id) {
            KeyEvent.KEY_PRESSED -> when {
                findShortcuts.contains(KeyStroke.getKeyStrokeForEvent(event)) -> {
                    consumedKey = event.keyCode
                    open()
                    true
                }
                isOpen && event.keyCode == KeyEvent.VK_ESCAPE -> {
                    consumedKey = event.keyCode
                    close()
                    true
                }
                else -> { consumedKey = KeyEvent.VK_UNDEFINED; false }
            }
            KeyEvent.KEY_TYPED -> consumedKey != KeyEvent.VK_UNDEFINED
            KeyEvent.KEY_RELEASED -> if (event.keyCode == consumedKey) {
                consumedKey = KeyEvent.VK_UNDEFINED
                true
            } else false
            else -> false
        }
        if (handled) event.consume()
        return handled
    }

    fun open() {
        if (!isOpen) {
            isOpen = true
            previousSelection = panel.selection
            host.add(bar, BorderLayout.NORTH)
            buffer.addModelListener(listener)
            timer.start()
            applyAppearance()
            host.revalidate()
            host.repaint()
            queryChanged()
        }
        bar.focusQuery()
        // First mount can precede the native layout in SwingPanel.
        SwingUtilities.invokeLater {
            val owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            if (isOpen && host.isShowing && owner != null && SwingUtilities.isDescendingFrom(owner, host)) {
                bar.focusQuery(selectAll = false)
            }
        }
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        timer.stop()
        buffer.removeModelListener(listener)
        host.remove(bar)
        panel.setFindResult(null)
        if (searchSelection != null && panel.selection === searchSelection) restoreSelection(previousSelection)
        searchSelection = null
        previousSelection = null
        host.revalidate()
        host.repaint()
        panel.requestFocusInWindow()
    }

    private fun escape() {
        consumedKey = KeyEvent.VK_ESCAPE
        close()
    }

    fun dispose() {
        timer.stop()
        buffer.removeModelListener(listener)
    }

    fun applyAppearance() = bar.applyAppearance(panel.background, panel.foreground)

    private fun queryChanged() {
        changedQuery = true
        dirty.set(true)
        if (bar.queryField.text.isEmpty()) {
            panel.setFindResult(null)
            if (searchSelection != null && panel.selection === searchSelection) restoreSelection(previousSelection)
            searchSelection = null
            dirty.set(false)
        }
        bar.showResults(null, bar.queryField.text.isNotEmpty())
    }

    /** Also used before navigation so a key never chooses stale coordinates after process output. */
    fun refreshNow() {
        if (!isOpen || !dirty.get() || !buffer.tryLock()) return
        try {
            refreshLocked()
        } finally {
            buffer.unlock()
        }
    }

    private fun refreshLocked() {
        if (!dirty.getAndSet(false)) return
        val reset = changedQuery
        changedQuery = false
        val previous = panel.findResult?.takeIf { it.items.isNotEmpty() }?.selectedItem()
        val previousIndex = if (reset) 0 else (previous?.index?.minus(1) ?: 0)
        val result = TerminalBufferSearch.find(buffer, bar.queryField.text)
        val count = result?.items?.size ?: 0
        if (count > 0) {
            val samePosition = if (reset || previous == null) -1 else result!!.items.indexOfFirst {
                it.start == previous.start && it.text == previous.text
            }
            repeat((samePosition.takeIf { it >= 0 } ?: previousIndex).coerceIn(0, count - 1)) { result!!.nextFindItem() }
        }
        panel.setFindResult(result)
        if (reset && count > 0) {
            // JediTerm starts at item zero but reveals only when navigating. Reveal that first result immediately.
            result!!.prevFindItem()
            panel.selectNextFindResultItem()
            searchSelection = panel.selection
        } else if (count == 0 && searchSelection != null && panel.selection === searchSelection) {
            restoreSelection(previousSelection)
            searchSelection = null
        } else if (count > 0 && searchSelection != null && panel.selection === searchSelection) {
            // Refresh active-match coordinates after screen/history changes without jumping the user's viewport.
            val item = result!!.selectedItem()
            val history = buffer.historyLinesCount
            restoreSelection(TerminalSelection(
                com.jediterm.core.compatibility.Point(item.start.x, item.start.y - history),
                com.jediterm.core.compatibility.Point(item.end.x, item.end.y - history),
            ))
            searchSelection = panel.selection
        }
        bar.showResults(result, false)
    }

    private fun navigate(backward: Boolean) {
        if (!isOpen || !buffer.tryLock()) return
        try {
            refreshLocked()
            val result = if (backward) panel.selectPrevFindResultItem() else panel.selectNextFindResultItem()
            searchSelection = panel.selection
            bar.showResults(result, false)
        } finally {
            buffer.unlock()
        }
    }
}
