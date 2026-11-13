package app.thdev.moru.feature.terminal

import com.jediterm.terminal.TerminalStarter
import com.jediterm.terminal.DefaultTerminalCopyPasteHandler
import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.SubstringFinder
import com.jediterm.terminal.emulator.mouse.MouseMode
import com.jediterm.terminal.emulator.mouse.TerminalMouseListener
import com.jediterm.terminal.model.StyleState
import com.jediterm.terminal.model.TerminalSelection
import com.jediterm.terminal.model.TerminalTextBuffer
import com.jediterm.terminal.ui.TerminalPanel
import com.jediterm.terminal.ui.TerminalAction
import com.jediterm.terminal.ui.settings.SettingsProvider
import com.jediterm.terminal.util.CharUtils
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.event.FocusEvent
import java.awt.event.InputMethodEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.font.TextHitInfo
import java.awt.im.InputMethodRequests
import java.beans.PropertyChangeListener
import java.lang.reflect.Method
import javax.swing.JScrollBar

/**
 * Native preedit UI adaptation around JediTerm's unchanged terminal rendering and committed-input transport.
 */
internal class MoruTerminalPanel(
    settings: SettingsProvider,
    private val buffer: TerminalTextBuffer,
    style: StyleState,
    copyToClipboard: (String) -> Unit = { DefaultTerminalCopyPasteHandler().setContents(it, false) },
    private val onFocusRequest: () -> Unit = {},
    private val endComposition: (MoruTerminalPanel) -> Unit = { it.inputContext?.endComposition() },
    private val onFindKey: (KeyEvent) -> Boolean = { false },
) : TerminalPanel(settings, buffer, style) {
    private val preedit = TerminalPreedit()
    private val clipboard = TerminalClipboardActions(this, copyToClipboard)
    @Volatile private var mouseMode = MouseMode.MOUSE_REPORTING_NONE

    /** Claude Code and Codex: the alternate screen has no history to reveal, but the CLI scrolls its own view. */
    private val agentScrolls = { buffer.isUsingAlternateBuffer && mouseMode != MouseMode.MOUSE_REPORTING_NONE }
    private val mouseActions = TerminalMouseActions(agentScrolls)
    private val selectionAutoScroll = TerminalSelectionAutoScroll(
        this,
        { mouseActions.selecting },
        agentScrolls,
        { super.processMouseMotionEvent(it) },
        { restoreSelection.invoke(this, it) },
    )
    @Volatile private var displayCursor = Point()
    @Volatile private var starter: TerminalStarter? = null
    private val modifiedEnter = TerminalModifiedEnter { starter?.sendString(it, true) }
    @Volatile private var bracketedPaste = false

    /**
     * macOS keeps the last Korean syllable composing until the next key, and AWT only deactivates the input method when
     * focus leaves, so that syllable never reached the CLI. Ending the composition while this panel still owns the input
     * context makes the input method commit it here first. The focus owner changes before FOCUS_LOST reaches the panel.
     */
    private val commitOnFocusLeave = PropertyChangeListener { event ->
        if (event.oldValue === this && preedit.active) endComposition(this)
    }

    init {
        transferHandler = TerminalDropHandler(::paste)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", commitOnFocusLeave)
    }

    override fun setTerminalStarter(terminalStarter: TerminalStarter) {
        starter = terminalStarter
        super.setTerminalStarter(terminalStarter)
    }

    /** Measures the settings' font again and resizes the grid to it, as JediTerm does when its font is zoomed. */
    fun refreshFont() = reinitFontAndResize()

    /** Restore the user's selection only when closing the finder still owns the temporary match selection. */
    internal fun restoreSearchSelection(selection: TerminalSelection?) = restoreSelection.invoke(this, selection).let { Unit }

    override fun selectPrevOrNextFindResultItem(next: Boolean): SubstringFinder.FindResult? {
        buffer.lock()
        return try {
            // JediTerm otherwise waits for paint to update history bounds and apply queued scroll deltas.
            // A result selected before that paint would clamp to the screen or move again on the next paint.
            flushScrollModel.invoke(this, false)
            super.selectPrevOrNextFindResultItem(next)
        } finally {
            buffer.unlock()
        }
    }

    override fun setBracketedPasteMode(bracketedPasteModeEnabled: Boolean) {
        bracketedPaste = bracketedPasteModeEnabled
        super.setBracketedPasteMode(bracketedPasteModeEnabled)
    }

    /**
     * Sends dropped or handed-over text the way JediTerm pastes, so an agent CLI sees a paste: it attaches the image
     * paths in it and keeps several lines as one message. False when the terminal has not started.
     */
    fun paste(text: String): Boolean {
        val terminal = starter ?: return false
        val lines = text.replace("\r\n", "\n").replace('\n', '\r')
        terminal.sendString(if (bracketedPaste) "\u001b[200~$lines\u001b[201~" else lines, true)
        requestFocusInWindow()
        return true
    }

    override fun requestFocusInWindow(): Boolean {
        onFocusRequest()
        return super.requestFocusInWindow()
    }

    // Fitted fonts by the font JediTerm picked and the cell width they were fitted to; a font or size change makes new ones.
    private val wideFonts = HashMap<Pair<Font, Int>, Font>()

    /** A two-cell character is drawn with a font fitted to its two cells (see [wideGlyphFont]). */
    override fun getFontToDisplay(text: CharArray, start: Int, end: Int, style: TextStyle): Font {
        val base = super.getFontToDisplay(text, start, end, style)
        if (end <= start || !CharUtils.isDoubleWidthCharacter(Character.codePointAt(text, start), false)) return base
        val cellWidth = myCharSize.width
        return wideFonts.getOrPut(base to cellWidth) {
            wideGlyphFont(base, getFontMetrics(base).charWidth(Character.codePointAt(text, start)).toFloat(), cellWidth)
        }
    }

    override fun processInputMethodEvent(event: InputMethodEvent) {
        preedit.update(event, columns(), ::inputOrigin)
        // Never give the private, one-cell JediTerm preedit painter a composing suffix or a caret-only event.
        if (event.id == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED && event.committedCharacterCount > 0) {
            super.processInputMethodEvent(event)
        }
        event.consume()
        repaint()
    }

    override fun handleKeyEvent(event: KeyEvent) {
        if (onFindKey(event)) return
        if (clipboard.handleKeyEvent(event)) return
        if (preedit.active) {
            if (event.id == KeyEvent.KEY_PRESSED) TerminalAction.processEvent(this, event)
            event.consume()
            return
        }
        preedit.forgetCommittedOrigin()
        if (modifiedEnter.handleKeyEvent(event)) return
        super.handleKeyEvent(event)
    }

    override fun processFocusEvent(event: FocusEvent) {
        if (event.id == FocusEvent.FOCUS_LOST) {
            preedit.forgetCommittedOrigin()
            modifiedEnter.reset()
            clipboard.reset()
            mouseActions.reset()
            selectionAutoScroll.stop()
        }
        // Focus can arrive here only with a new input method session, so marked text still shown is a syllable whose
        // commit went to the pane that took focus, or nowhere. Left in place it keeps the panel composing and swallows
        // every key. The commit for a syllable may still land after FOCUS_LOST, so this waits for the return.
        if (event.id == FocusEvent.FOCUS_GAINED && preedit.active) {
            preedit.abandon()
            repaint()
        }
        super.processFocusEvent(event)
    }

    override fun processMouseEvent(event: MouseEvent) {
        if (event.id == MouseEvent.MOUSE_RELEASED) selectionAutoScroll.followAgentScroll()
        if (event.id == MouseEvent.MOUSE_PRESSED || event.id == MouseEvent.MOUSE_RELEASED) selectionAutoScroll.stop()
        if (clipboard.handleMouseEvent(event)) return
        if (event.id == MouseEvent.MOUSE_PRESSED) {
            // A click ends typing at the composing syllable, as in a native text view.
            if (preedit.active) endComposition(this) else preedit.forgetCommittedOrigin()
        }
        mouseActions.button(event) { super.processMouseEvent(it) }
    }

    override fun processMouseMotionEvent(event: MouseEvent) {
        if (mouseActions.selecting) selectionAutoScroll.followAgentScroll()
        mouseActions.dragged(event)
        super.processMouseMotionEvent(event)
        selectionAutoScroll.dragged(event)
    }

    override fun terminalMouseModeSet(mode: MouseMode) {
        mouseMode = mode
        super.terminalMouseModeSet(mode)
    }

    override fun processMouseWheelEvent(event: MouseWheelEvent) {
        if (!mouseActions.selecting || !agentScrolls() || event.isAltDown) return super.processMouseWheelEvent(event)
        selectionAutoScroll.beforeAgentScroll()
        // JediTerm drops the selection before it reports a wheel; the held selection is still being made.
        val held = selection
        super.processMouseWheelEvent(event)
        if (held != null && selection == null) restoreSelection.invoke(this, held)
    }

    override fun handleMouseWheelEvent(event: MouseWheelEvent, scrollBar: JScrollBar) {
        if (mouseActions.isSelectionWheel(event)) {
            selectionAutoScroll.wheel(event)
        } else super.handleMouseWheelEvent(event, scrollBar)
    }

    /** JediTerm drops the selection on every scroll; keep it on the text it covered (see [afterScroll]). */
    override fun scrollArea(scrollRegionTop: Int, scrollRegionSize: Int, dy: Int) {
        val kept = selection
        super.scrollArea(scrollRegionTop, scrollRegionSize, dy)
        val moved = kept?.afterScroll(scrollRegionTop, scrollRegionSize, dy, intoHistory = !buffer.isUsingAlternateBuffer)
        if (moved != null) restoreSelection.invoke(this, moved)
    }

    override fun addTerminalMouseListener(listener: TerminalMouseListener) {
        mouseActions.registerTerminalListener(this) { super.addTerminalMouseListener(listener) }
    }

    override fun isLocalMouseAction(event: MouseEvent): Boolean =
        isLocalGesture(event) || super.isLocalMouseAction(event)

    override fun isRemoteMouseAction(event: MouseEvent): Boolean =
        !isLocalGesture(event) && super.isRemoteMouseAction(event)

    private fun isLocalGesture(event: MouseEvent): Boolean = mouseActions.dispatchingLocally ||
        mouseActions.isSelectionWheel(event) ||
        (event.id == MouseEvent.MOUSE_WHEEL && !buffer.isUsingAlternateBuffer && !event.isAltDown) ||
        (TerminalClipboardActions.isSelectionGesture(event) && !event.isAltDown)

    override fun dispose() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", commitOnFocusLeave)
        selectionAutoScroll.stop()
        mouseActions.reset()
        super.dispose()
    }

    override fun setCursor(x: Int, y: Int) {
        displayCursor = Point(x, y - 1)
        super.setCursor(x, y)
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val layout = preedit.layout(columns()) ?: return
        val overlay = graphics.create() as Graphics2D
        try {
            setupAntialiasing(overlay)
            layout.paint(overlay, myCharSize, insetX, foreground, background) { chars ->
                getFontToDisplay(chars, 0, chars.size, TextStyle.EMPTY)
            }
        } finally {
            overlay.dispose()
        }
    }

    override fun getInputMethodRequests(): InputMethodRequests {
        val native = super.getInputMethodRequests()
        return object : InputMethodRequests by native {
            override fun getTextLocation(offset: TextHitInfo?): java.awt.Rectangle {
                val layout = preedit.layout(columns())
                if (layout == null && isShowing) return native.getTextLocation(offset)
                val location = (layout ?: TerminalPreeditLayout("", inputOrigin(), columns()))
                    .location(offset?.insertionIndex ?: preedit.caret, myCharSize, insetX)
                if (isShowing) location.translate(locationOnScreen.x, locationOnScreen.y)
                return location
            }
        }
    }

    private companion object {
        // JediTerm offers no public way to set a selection; this is the setter its own mouse handling uses.
        val restoreSelection: Method = TerminalPanel::class.java
            .getDeclaredMethod("updateSelection", TerminalSelection::class.java)
            .apply { isAccessible = true }

        val flushScrollModel: Method = TerminalPanel::class.java
            .getDeclaredMethod("updateScrolling", Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
    }

    private fun columns(): Int = ((pixelWidth - insetX) / myCharSize.width.coerceAtLeast(1)).coerceAtLeast(1)

    private fun inputOrigin(): Point {
        if (!isShowing) return Point(displayCursor)
        buffer.lock()
        try {
            val location = super.getInputMethodRequests().getTextLocation(null)
            val screen = locationOnScreen
            return Point((location.x - screen.x - insetX) / myCharSize.width, (location.y - screen.y) / myCharSize.height - 1)
        } finally {
            buffer.unlock()
        }
    }
}
