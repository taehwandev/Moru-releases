package app.thdev.moru.feature.terminal

import com.jediterm.terminal.ui.TerminalPanel
import java.awt.event.MouseEvent
import kotlin.math.abs
import javax.swing.SwingUtilities

/**
 * Keeps selection gestures local while preserving deliberate CLI mouse actions. When [agentScrolls], the alternate
 * screen has no history to scroll locally, so a held selection's wheel still reaches the CLI.
 */
internal class TerminalMouseActions(private val agentScrolls: () -> Boolean = { false }) {
    private var pendingPress: MouseEvent? = null
    private var dragging = false
    private var localPress = false
    private var cancelledRelease = false
    private val wheelNotches = TerminalWheelNotches()
    var dispatchingLocally = false
        private set
    val selecting: Boolean get() = localPress && dragging

    fun isSelectionWheel(event: MouseEvent): Boolean =
        event.id == MouseEvent.MOUSE_WHEEL && selecting && !event.isAltDown && !agentScrolls()

    fun registerTerminalListener(panel: TerminalPanel, register: () -> Unit) {
        val existing = panel.mouseWheelListeners.toSet()
        register()
        // Keep observers and JediTerm's local handler; suppress only its remote/fallback wheel callbacks, and feed
        // those whole notches because they drop a fractional trackpad event.
        panel.mouseWheelListeners.filterNot(existing::contains).forEach { listener ->
            panel.removeMouseWheelListener(listener)
            panel.addMouseWheelListener { event ->
                if (!isSelectionWheel(event)) wheelNotches.split(event).forEach(listener::mouseWheelMoved)
            }
        }
    }

    fun button(event: MouseEvent, dispatch: (MouseEvent) -> Unit) {
        if (!SwingUtilities.isLeftMouseButton(event)) {
            dispatch(event)
            return
        }
        when (event.id) {
            MouseEvent.MOUSE_PRESSED -> {
                cancelledRelease = false
                dragging = false
                localPress = !event.isAltDown && !event.isControlDown && !event.isMetaDown
                pendingPress = event.takeIf { localPress && it.clickCount == 1 && !it.isShiftDown }
                if (localPress) locally { dispatch(event) } else dispatch(event)
            }
            MouseEvent.MOUSE_RELEASED -> {
                if (cancelledRelease) {
                    cancelledRelease = false
                    locally { dispatch(event) }
                    return
                }
                val press = pendingPress
                pendingPress = null
                if (localPress && (dragging || press == null)) {
                    locally { dispatch(event) }
                } else if (press != null) {
                    dispatch(press)
                    dispatch(event)
                } else dispatch(event)
                dragging = false
                localPress = false
            }
            else -> dispatch(event)
        }
    }

    fun dragged(event: MouseEvent) {
        if (event.id == MouseEvent.MOUSE_DRAGGED && SwingUtilities.isLeftMouseButton(event)) {
            // Trackpads can report a tiny drag during a click. Keep the press pending so fullscreen CLIs still get it.
            val press = pendingPress
            if (press == null || abs(event.x - press.x) >= 4 || abs(event.y - press.y) >= 4) dragging = true
        }
        // A move without a held button means the press ended even if its release never reached the terminal;
        // a press left over would turn every later wheel into a local selection scroll and starve the CLI.
        if (event.id == MouseEvent.MOUSE_MOVED && localPress) reset()
    }

    fun reset() {
        wheelNotches.reset()
        cancelledRelease = cancelledRelease || localPress
        pendingPress = null
        dragging = false
        localPress = false
    }

    private inline fun locally(action: () -> Unit) {
        dispatchingLocally = true
        try { action() } finally { dispatchingLocally = false }
    }
}
