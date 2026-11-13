package app.thdev.moru.feature.terminal

import java.awt.GraphicsEnvironment
import java.awt.event.FocusEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import org.junit.Assume.assumeFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerminalNativeSelectionTest {
    @Test
    fun nativeMouseAndKeyboardFocusSelectThePaneWithoutRecreatingSessions() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        TerminalEfficiencyWindow().use { fixture ->
            fixture.waitForSession(fixture.first)
            val second = fixture.addSession()
            val third = fixture.addSession()
            fixture.waitForSession(third)
            fixture.manager.selectSession(fixture.first)
            fixture.waitUntil("first terminal selected") { fixture.keyboardRequests(fixture.first) > 1 }
            val retained = fixture.manager.listSessions().map { it.id }
            SwingUtilities.invokeAndWait {
                val panel = JediTermSessionRegistry.find(third)!!.session.focusTarget()
                assertTrue(panel is MoruTerminalPanel, "Native input events belong to the terminal body, not its focus proxy")
                panel.dispatchEvent(MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    0, 20, 20, 1, false, MouseEvent.BUTTON1))
                panel.dispatchEvent(MouseEvent(panel, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                    0, 20, 20, 1, false, MouseEvent.BUTTON1))
            }
            fixture.waitUntil("native third terminal selected") { fixture.manager.selectedSessionId() == third }
            SwingUtilities.invokeAndWait {
                val oldPanel = JediTermSessionRegistry.find(fixture.first)!!.session.focusTarget()
                oldPanel.focusListeners.forEach {
                    it.focusGained(FocusEvent(oldPanel, FocusEvent.FOCUS_GAINED, false, null, FocusEvent.Cause.ACTIVATION))
                }
            }
            assertEquals(third, fixture.manager.selectedSessionId(), "Returning from a popup must not undo a newer selection")
            SwingUtilities.invokeAndWait {
                val panel = JediTermSessionRegistry.find(second)!!.session.focusTarget()
                // A keyboard focus transition has no Compose mouse click to notify the pane.
                panel.focusListeners.forEach {
                    it.focusGained(FocusEvent(panel, FocusEvent.FOCUS_GAINED, false, null, FocusEvent.Cause.TRAVERSAL_FORWARD))
                }
            }
            fixture.waitUntil("keyboard-focused second terminal selected") { fixture.manager.selectedSessionId() == second }
            assertEquals(retained, fixture.manager.listSessions().map { it.id })
        }
    }
}
