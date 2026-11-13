package app.thdev.moru.feature.terminal

import com.jediterm.terminal.model.SelectionUtil
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TerminalSelectionNavigationTest {
    @Test
    fun commandASelectsTheWholeHistoryWithoutSendingItsTypedTail() {
        PreeditTestFixture().use { fixture ->
            records(fixture)
            onEdt {
                val panel = fixture.panel
                panel.processKeyEvent(KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, InputEvent.META_DOWN_MASK, KeyEvent.VK_A, 'a'))
                panel.processKeyEvent(KeyEvent(panel, KeyEvent.KEY_TYPED, 1, 0, KeyEvent.VK_UNDEFINED, 'a'))
                panel.processKeyEvent(KeyEvent(panel, KeyEvent.KEY_RELEASED, 2, 0, KeyEvent.VK_A, 'a'))
                assertEquals((0 until 80).joinToString("\n", transform = ::record), selected(fixture).trimEnd())
            }
            assertFalse(fixture.connector.hasWriteWithin(150), "Select all must not type an a or control byte into the CLI.")
        }
    }

    @Test
    fun aStationaryDragOnePixelAboveTheViewportContinuesIntoHistoryUntilRelease() {
        PreeditTestFixture().use { fixture ->
            records(fixture)
            val startRecord = onEdt {
                val value = recordAt(fixture, 3)
                mouse(fixture, MouseEvent.MOUSE_PRESSED, record(value).length, rowY(fixture, 3))
                mouse(fixture, MouseEvent.MOUSE_DRAGGED, 0, -1)
                value
            }
            await { onEdt { fixture.panel.verticalScrollModel.value <= -3 } }
            val stopped = onEdt {
                mouse(fixture, MouseEvent.MOUSE_RELEASED, 0, -1)
                val origin = fixture.panel.verticalScrollModel.value
                val first = recordAt(fixture, origin)
                assertEquals((first..startRecord).joinToString("\n", transform = ::record), selected(fixture))
                origin
            }
            Thread.sleep(200)
            assertEquals(stopped, onEdt { fixture.panel.verticalScrollModel.value }, "Mouse release must stop the timer.")
            assertFalse(fixture.connector.hasWriteWithin(150), "Edge selection must remain local in a mouse-reporting CLI.")
        }
    }

    @Test
    fun bottomEdgeDraggingContinuesDownwardAndReturningInsideStopsIt() {
        PreeditTestFixture().use { fixture ->
            records(fixture)
            // Flush JediTerm's pending output-scroll delta before choosing a nonzero history origin.
            fixture.paint()
            val origin = onEdt {
                fixture.panel.verticalScrollModel.value = -30
                val before = fixture.panel.verticalScrollModel.value
                assertTrue(before <= -3, "The fixture must begin in history with room to scroll downward.")
                mouse(fixture, MouseEvent.MOUSE_PRESSED, 0, rowY(fixture, 2))
                mouse(fixture, MouseEvent.MOUSE_DRAGGED, record(0).length, minOf(fixture.panel.height, fixture.panel.pixelHeight) + 1)
                before
            }
            await { onEdt { fixture.panel.verticalScrollModel.value >= origin + 3 } }
            val stopped = onEdt {
                val last = recordAt(fixture, fixture.panel.verticalScrollModel.value + fixture.panel.terminalTextBuffer.height - 1)
                assertTrue(selected(fixture).endsWith(record(last)), "The selection must follow the bottom edge after scrolling.")
                mouse(fixture, MouseEvent.MOUSE_DRAGGED, record(0).length, rowY(fixture, 5))
                fixture.panel.verticalScrollModel.value
            }
            Thread.sleep(200)
            assertEquals(stopped, onEdt { fixture.panel.verticalScrollModel.value })
            onEdt { mouse(fixture, MouseEvent.MOUSE_RELEASED, record(0).length, rowY(fixture, 5)) }
            assertFalse(fixture.connector.hasWriteWithin(150))
        }
    }

    @Test
    fun selectionWheelInTheAlternateScreenReportsOnlyAWheelAndKeepsTheSelection() {
        for (reporting in listOf(true, false)) {
            PreeditTestFixture().use { fixture ->
                fixture.output("\u001B[?1049h\u001B[?1006h\u001B[?1003${if (reporting) 'h' else 'l'}\u001B[Hhello")
                onEdt {
                    val observed = mutableListOf<Int>()
                    fixture.panel.addMouseWheelListener { observed += it.wheelRotation }
                    mouse(fixture, MouseEvent.MOUSE_PRESSED, 0, rowY(fixture, 0))
                    mouse(fixture, MouseEvent.MOUSE_DRAGGED, 5, rowY(fixture, 0))
                    val wheel = MouseWheelEvent(fixture.panel, MouseEvent.MOUSE_WHEEL, 0,
                        InputEvent.BUTTON1_DOWN_MASK, 10, rowY(fixture, 0), 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, -1)
                    fixture.panel.dispatchEvent(wheel)
                    assertEquals(listOf(-1), observed, "Native wheel observers must still receive the selection gesture.")
                    assertEquals("hello", selected(fixture), "Wheel must preserve the visible alternate-buffer selection.")
                    mouse(fixture, MouseEvent.MOUSE_RELEASED, 5, rowY(fixture, 0))
                }
                if (reporting) {
                    // The CLI owns the alternate screen's scrolling: one wheel report, never Up/Down keys.
                    fixture.connector.hasWriteWithin(150)
                    val written = fixture.connector.writtenText()
                    assertTrue(Regex("\u001B\\[<64;\\d+;1M").matches(written), written)
                } else {
                    assertFalse(fixture.connector.hasWriteWithin(150), "Without mouse reporting the wheel must not send Up/Down keys.")
                }
            }
        }
    }

    @Test
    fun altDraggingKeepsTheRemoteGestureWithoutStartingLocalAutoScroll() {
        PreeditTestFixture().use { fixture ->
            records(fixture)
            onEdt {
                mouse(fixture, MouseEvent.MOUSE_PRESSED, 0, rowY(fixture, 3), InputEvent.ALT_DOWN_MASK)
                mouse(fixture, MouseEvent.MOUSE_DRAGGED, 0, -1, InputEvent.ALT_DOWN_MASK)
            }
            Thread.sleep(200)
            assertEquals(0, onEdt { fixture.panel.verticalScrollModel.value })
            assertTrue(fixture.connector.hasWriteWithin(150), "Alt drag must preserve the deliberate remote mouse gesture.")
            onEdt { mouse(fixture, MouseEvent.MOUSE_RELEASED, 0, -1, InputEvent.ALT_DOWN_MASK) }
        }
    }

    @Test
    fun ordinaryAndAltWheelsKeepTheSdkReportingAndAlternateArrowFallback() {
        for (reporting in listOf(true, false)) {
            for (modifiers in listOf(0, InputEvent.ALT_DOWN_MASK)) {
                PreeditTestFixture().use { fixture ->
                    fixture.output("\u001B[?1049h\u001B[?1003${if (reporting) 'h' else 'l'}\u001B[?1006h")
                    onEdt {
                        val observed = mutableListOf<Int>()
                        fixture.panel.addMouseWheelListener { observed += it.wheelRotation }
                        fixture.panel.dispatchEvent(MouseWheelEvent(fixture.panel, MouseEvent.MOUSE_WHEEL, 0,
                            modifiers, 10, rowY(fixture, 0), 0, false,
                            MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, -1))
                        assertEquals(listOf(-1), observed)
                    }
                    if (reporting) {
                        assertTrue(fixture.connector.hasWriteWithin(150), "Ordinary and Alt wheels must still reach the mouse-reporting CLI.")
                    } else {
                        assertTrue(fixture.connector.awaitText("\u001B[A".repeat(3)), "Unselected alternate-screen wheels must retain JediTerm's arrow fallback.")
                    }
                }
            }
        }
    }

    @Test
    fun focusLossAndDisposalStopTheTimerAndCancelTheOutstandingLocalRelease() {
        for (dispose in listOf(false, true)) {
            PreeditTestFixture().use { fixture ->
                records(fixture)
                onEdt {
                    mouse(fixture, MouseEvent.MOUSE_PRESSED, 5, rowY(fixture, 3))
                    mouse(fixture, MouseEvent.MOUSE_DRAGGED, 0, -1)
                }
                await { onEdt { fixture.panel.verticalScrollModel.value <= -2 } }
                val stopped = onEdt {
                    val panel = fixture.panel as MoruTerminalPanel
                    if (dispose) panel.dispose() else loseFocus(panel)
                    panel.verticalScrollModel.value
                }
                Thread.sleep(200)
                assertEquals(stopped, onEdt { fixture.panel.verticalScrollModel.value })
                onEdt { mouse(fixture, MouseEvent.MOUSE_RELEASED, 0, -1) }
                assertFalse(fixture.connector.hasWriteWithin(150), "The cancelled local drag must not leave an unmatched remote release.")
            }
        }
    }

    @Test
    fun fractionalSelectionWheelsAccumulateRowsAndResetBetweenDrags() {
        PreeditTestFixture().use { fixture ->
            records(fixture)
            onEdt {
                mouse(fixture, MouseEvent.MOUSE_PRESSED, 9, rowY(fixture, 3))
                mouse(fixture, MouseEvent.MOUSE_DRAGGED, 0, rowY(fixture, 3))
                fun fractionalWheel() {
                    val wheel = MouseWheelEvent(fixture.panel, MouseEvent.MOUSE_WHEEL, 0,
                        InputEvent.BUTTON1_DOWN_MASK, 4, rowY(fixture, 3), 4, rowY(fixture, 3), 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 0, -0.25)
                    fixture.panel.dispatchEvent(wheel)
                    assertTrue(wheel.isConsumed)
                }
                fractionalWheel()
                assertEquals(0, fixture.panel.verticalScrollModel.value)
                fractionalWheel()
                assertEquals(-1, fixture.panel.verticalScrollModel.value, "Precise rotations must accumulate even when wheelRotation is zero.")
                assertTrue(selected(fixture).contains(record(recordAt(fixture, 2))))
                mouse(fixture, MouseEvent.MOUSE_RELEASED, 0, rowY(fixture, 3))
                mouse(fixture, MouseEvent.MOUSE_PRESSED, 9, rowY(fixture, 3))
                mouse(fixture, MouseEvent.MOUSE_DRAGGED, 0, rowY(fixture, 3))
                fractionalWheel()
                assertEquals(-1, fixture.panel.verticalScrollModel.value, "The previous drag's fractional remainder must not carry into a new gesture.")
                mouse(fixture, MouseEvent.MOUSE_RELEASED, 0, rowY(fixture, 3))
            }
            assertFalse(fixture.connector.hasWriteWithin(150))
        }
    }

    private fun records(fixture: PreeditTestFixture) {
        fixture.output("\u001B[2J\u001B[H" + (0 until 80).joinToString("\r\n", transform = ::record) + "\u001B[?1003h\u001B[?1006h")
        onEdt {
            val buffer = fixture.panel.terminalTextBuffer
            assertTrue(buffer.historyLinesCount > 30)
            fixture.panel.verticalScrollModel.setRangeProperties(0, buffer.height, -buffer.historyLinesCount, buffer.height, false)
        }
    }

    private fun mouse(fixture: PreeditTestFixture, id: Int, column: Int, y: Int, modifiers: Int = 0) {
        val pressed = id != MouseEvent.MOUSE_RELEASED
        fixture.panel.dispatchEvent(MouseEvent(fixture.panel, id, 0,
            modifiers or if (pressed) InputEvent.BUTTON1_DOWN_MASK else 0,
            4 + column * fixture.cellWidth, y, 1, false,
            if (id == MouseEvent.MOUSE_DRAGGED) MouseEvent.NOBUTTON else MouseEvent.BUTTON1))
    }

    private fun rowY(fixture: PreeditTestFixture, row: Int): Int =
        (fixture.panel.pixelHeight / fixture.panel.terminalTextBuffer.height) * row + 1

    private fun selected(fixture: PreeditTestFixture): String {
        val selection = assertNotNull(fixture.panel.selection)
        return SelectionUtil.getSelectionText(selection, fixture.panel.terminalTextBuffer)
    }

    private fun recordAt(fixture: PreeditTestFixture, row: Int): Int =
        fixture.panel.terminalTextBuffer.getLine(row).text.removePrefix("record-").trim().toInt()

    private fun record(index: Int): String = "record-%02d".format(index)

    private fun loseFocus(panel: MoruTerminalPanel) {
        val method = MoruTerminalPanel::class.java.getDeclaredMethod("processFocusEvent", FocusEvent::class.java)
        method.isAccessible = true
        method.invoke(panel, FocusEvent(panel, FocusEvent.FOCUS_LOST))
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 3_000_000_000L
        while (System.nanoTime() < deadline && !condition()) Thread.sleep(20)
        assertTrue(condition(), "The stationary held drag did not continue scrolling.")
    }

    private fun <T> onEdt(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }
}
