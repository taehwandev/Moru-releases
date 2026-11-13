package app.thdev.moru.feature.terminal

import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

/** Codex and Claude Code switch to the alternate screen and ask for SGR mouse reports; the wheel must reach them. */
class TerminalAgentWheelTest {
    @Test
    fun aClickReachesAnAgentAsSgrPressAndRelease() {
        AgentScreen().use { screen ->
            screen.mouse(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 20, MouseEvent.BUTTON1)
            screen.mouse(MouseEvent.MOUSE_RELEASED, 0, 20, MouseEvent.BUTTON1)
            assertTrue(screen.fixture.connector.awaitText("\u001B[<0;3;2M\u001B[<0;3;2m"), screen.written())
        }
    }

    @Test
    fun aClickWithMinorPointerMovementStillReachesTheAgent() {
        AgentScreen().use { screen ->
            screen.mouse(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 20, MouseEvent.BUTTON1)
            screen.motion(MouseEvent.MOUSE_DRAGGED, InputEvent.BUTTON1_DOWN_MASK, 22)
            screen.mouse(MouseEvent.MOUSE_RELEASED, 0, 22, MouseEvent.BUTTON1)
            assertTrue(screen.fixture.connector.awaitText("\u001B[<0;3;2M\u001B[<0;3;2m"), screen.written())
        }
    }

    @Test
    fun aMouseWheelNotchReachesTheAgent() {
        AgentScreen().use { screen ->
            screen.wheel(rotation = -1, precise = -1.0)
            assertTrue(screen.fixture.connector.awaitText(WheelUp), screen.written())
        }
    }

    @Test
    fun fractionalTrackpadScrollingReachesTheAgentInWholeNotches() {
        AgentScreen().use { screen ->
            repeat(3) { screen.wheel(rotation = 0, precise = -0.4) }
            assertTrue(screen.fixture.connector.awaitText(WheelUp), screen.written())
            repeat(5) { screen.wheel(rotation = 0, precise = 0.5) }
            assertTrue(screen.fixture.connector.awaitText(WheelUp + WheelDown + WheelDown), screen.written())
        }
    }

    @Test
    fun aDragWhoseReleaseWasLostStopsCapturingTheWheelOnceTheMouseMoves() {
        AgentScreen().use { screen ->
            screen.mouse(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 20, MouseEvent.BUTTON1)
            screen.motion(MouseEvent.MOUSE_DRAGGED, InputEvent.BUTTON1_DOWN_MASK, 60)
            screen.motion(MouseEvent.MOUSE_MOVED, 0, 70)
            val before = screen.written()
            screen.wheel(rotation = -1, precise = -1.0)
            assertTrue(screen.fixture.connector.awaitText(before + WheelUp), screen.written())
        }
    }

    @Test
    fun aHeldSelectionStillScrollsTheAgentBecauseTheAlternateScreenHasNoHistory() {
        AgentScreen().use { screen ->
            screen.mouse(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 20, MouseEvent.BUTTON1)
            screen.motion(MouseEvent.MOUSE_DRAGGED, InputEvent.BUTTON1_DOWN_MASK, 60)
            val before = screen.written()
            screen.wheel(rotation = -1, precise = -1.0)
            assertTrue(screen.fixture.connector.awaitText(before + WheelUp), screen.written())
            assertTrue(onEdt { screen.fixture.panel.selection != null }, "The held selection must survive the wheel")
        }
    }
}

private const val WheelUp = "\u001B[<64;3;2M"
private const val WheelDown = "\u001B[<65;3;2M"

private class AgentScreen : AutoCloseable {
    val fixture = PreeditTestFixture()

    init {
        fixture.output("\u001B[?1049h\u001B[?1000h\u001B[?1002h\u001B[?1003h\u001B[?1006h")
    }

    fun wheel(rotation: Int, precise: Double) = onEdt {
        fixture.panel.dispatchEvent(MouseWheelEvent(fixture.panel, MouseEvent.MOUSE_WHEEL, 0, 0, 20, 20, 20, 20, 0, false,
            MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation, precise))
    }

    fun mouse(id: Int, modifiers: Int, x: Int, button: Int) = onEdt {
        fixture.panel.dispatchEvent(MouseEvent(fixture.panel, id, 0, modifiers, x, 20, 1, false, button))
    }

    fun motion(id: Int, modifiers: Int, x: Int) = mouse(id, modifiers, x, MouseEvent.NOBUTTON)

    fun written() = fixture.connector.writtenText()

    override fun close() = fixture.close()
}

private fun <T> onEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    var result: Result<T>? = null
    SwingUtilities.invokeAndWait { result = runCatching(block) }
    return requireNotNull(result).getOrThrow()
}
