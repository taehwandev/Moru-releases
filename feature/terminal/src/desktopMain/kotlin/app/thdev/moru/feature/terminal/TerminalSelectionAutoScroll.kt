package app.thdev.moru.feature.terminal

import com.jediterm.core.compatibility.Point
import com.jediterm.terminal.model.TerminalSelection
import com.jediterm.terminal.ui.TerminalPanel
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.Timer
import kotlin.math.abs

/**
 * Repeats edge scrolling while a local selection drag stays outside the terminal viewport. When [agentScrolls], the
 * alternate screen has no history, so the CLI is asked to scroll with a wheel report and the selection's anchor follows
 * its text as the CLI redraws (see [screenShift]).
 */
internal class TerminalSelectionAutoScroll(
    private val panel: TerminalPanel,
    private val isSelecting: () -> Boolean,
    private val agentScrolls: () -> Boolean,
    private val extendSelection: (MouseEvent) -> Unit,
    private val moveSelection: (TerminalSelection) -> Unit,
) {
    private var drag: MouseEvent? = null
    private var wheelRemainder = 0.0
    private var agentScreen: AgentScreen? = null
    private val timer = Timer(75) { scrollAtEdge() }

    fun dragged(event: MouseEvent) {
        if (!isSelecting() || event.isAltDown || event.y in 0 until viewportHeight()) {
            stop()
            return
        }
        drag = event
        if (!timer.isRunning) timer.start()
    }

    fun wheel(event: MouseWheelEvent) {
        val before = panel.verticalScrollModel.value
        if (!event.isShiftDown && event.preciseWheelRotation.isFinite()) {
            val accumulated = wheelRemainder + event.preciseWheelRotation * event.scrollAmount
            val rows = accumulated.toInt()
            wheelRemainder = accumulated - rows
            panel.verticalScrollModel.value += rows
        }
        if (panel.verticalScrollModel.value != before) extend(event)
        // JediTerm otherwise reports a remote wheel or synthesizes arrow keys in its alternate screen.
        event.consume()
    }

    /** Call before a wheel reaches a CLI on the alternate screen while the selection is held. */
    fun beforeAgentScroll() {
        followAgentScroll()
        agentScreen = AgentScreen(screenRows(), panel.selection?.start ?: return)
    }

    /** Moves the selection's anchor onto the text the CLI redrew elsewhere since the last scroll it was asked for. */
    fun followAgentScroll() {
        val last = agentScreen ?: return
        val selection = panel.selection
        // JediTerm or a scroll the CLI did itself (see afterScroll) already moved the anchor.
        if (selection == null || selection.start != last.anchor) {
            agentScreen = null
            return
        }
        val rows = screenRows()
        val dy = screenShift(last.rows, rows)
        val row = selection.start.y + dy
        val start = when {
            row < 0 -> Point(0, 0)
            row >= rows.size -> Point(panel.terminalTextBuffer.width, rows.size - 1)
            else -> Point(selection.start.x, row)
        }
        if (dy != 0) moveSelection(TerminalSelection(start, selection.end))
        agentScreen = AgentScreen(rows, start)
    }

    fun stop() {
        timer.stop()
        drag = null
        wheelRemainder = 0.0
        agentScreen = null
    }

    private fun scrollAtEdge() {
        val event = drag ?: return
        val height = viewportHeight()
        if (!isSelecting() || panel.selection == null || height <= 0) {
            stop()
            return
        }
        val outside = if (event.y < 0) event.y else event.y - height + 1
        val rowHeight = (panel.pixelHeight / panel.terminalTextBuffer.height.coerceAtLeast(1)).coerceAtLeast(1)
        if (agentScrolls()) {
            followAgentScroll()
            extend(event)
            scrollAgent(event, if (outside < 0) -1 else 1, rowHeight)
            return
        }
        val step = (abs(outside) / rowHeight + 1).coerceAtMost(6) * if (outside < 0) -1 else 1
        val before = panel.verticalScrollModel.value
        panel.verticalScrollModel.value += step
        if (panel.verticalScrollModel.value == before) {
            extend(event)
            stop()
            return
        }
        extend(event)
    }

    private fun extend(event: MouseEvent) {
        extendSelection(MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, event.`when`,
            event.modifiersEx or InputEvent.BUTTON1_DOWN_MASK,
            event.x.coerceIn(0, panel.pixelWidth.coerceAtLeast(1) - 1),
            event.y.coerceIn(0, viewportHeight().coerceAtLeast(1) - 1),
            0, false, MouseEvent.NOBUTTON))
    }

    /** One wheel notch over the anchor's row, so a CLI with a fixed input box scrolls its transcript, not the box. */
    private fun scrollAgent(event: MouseEvent, direction: Int, rowHeight: Int) {
        val anchor = panel.selection?.start ?: return
        val y = ((anchor.y - panel.verticalScrollModel.value) * rowHeight + rowHeight / 2)
            .coerceIn(0, viewportHeight().coerceAtLeast(1) - 1)
        val x = event.x.coerceIn(0, panel.pixelWidth.coerceAtLeast(1) - 1)
        panel.dispatchEvent(MouseWheelEvent(panel, MouseEvent.MOUSE_WHEEL, event.`when`, 0, x, y, 0, 0, 0, false,
            MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, direction, direction.toDouble()))
    }

    private fun screenRows(): List<String> {
        val buffer = panel.terminalTextBuffer
        buffer.lock()
        try {
            return List(buffer.height) { buffer.getLine(it).text }
        } finally {
            buffer.unlock()
        }
    }

    private class AgentScreen(val rows: List<String>, val anchor: Point)

    private fun viewportHeight(): Int = minOf(panel.height, panel.pixelHeight).coerceAtLeast(0)
}
