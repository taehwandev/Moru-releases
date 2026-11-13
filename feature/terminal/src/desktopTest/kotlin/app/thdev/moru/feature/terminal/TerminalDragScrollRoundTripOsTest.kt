package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import java.awt.Desktop
import java.awt.Point
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import com.jediterm.terminal.model.SelectionUtil
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The same gesture through the real pointer, wheel and keyboard. Needs the machine to itself. */
class TerminalDragScrollRoundTripOsTest {
    @Test
    fun aHeldDragScrollCopiesAndPastesAllTheSelectedHistoryLines() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val previous = clipboard.getContents(null)
        clipboard.setContents(StringSelection("no-selection-copied"), null)
        val layers = System.getProperty("compose.layers.type")
        System.setProperty("compose.layers.type", "WINDOW")
        val robot = Robot().apply { autoDelay = 35 }
        var window: ComposeWindow? = null
        var buttonDown = false
        PreeditTestFixture().use { fixture ->
            try {
                onEdt {
                    window = ComposeWindow().apply {
                        setSize(640, 340)
                        setLocation(80, 80)
                        setContent { SwingPanel(factory = { fixture.widget }, modifier = Modifier.fillMaxSize()) }
                        isVisible = true
                        toFront()
                    }
                }
                waitFor { onEdt { fixture.panel.isShowing && fixture.panel.width > 100 } }
                Desktop.getDesktop().requestForeground(true)
                onEdt {
                    window!!.toFront()
                    window.requestFocus()
                    fixture.panel.requestFocusInWindow()
                }
                val firstClick = onEdt { fixture.panel.locationOnScreen }
                robot.mouseMove(firstClick.x + 20, firstClick.y + 20)
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                robot.waitForIdle()
                waitFor { onEdt { fixture.panel.isFocusOwner } }
                fixture.output("\u001B[?2004h" + (1..80).joinToString("") { "record-%02d\r\n".format(it) } + "\u001B[?1002h")
                waitFor { onEdt { fixture.panel.verticalScrollModel.minimum < 0 } }
                val startRow = onEdt { fixture.panel.terminalTextBuffer.height - 3 }
                val startRecord = onEdt { recordAt(fixture, startRow) }
                val drags = AtomicInteger()
                val wheels = AtomicInteger()
                val rotation = AtomicInteger()
                val wheelDetail = AtomicReference("")
                val lastDragX = AtomicInteger(Int.MAX_VALUE)
                onEdt {
                    fixture.panel.addMouseMotionListener(object : MouseMotionAdapter() {
                        override fun mouseDragged(e: MouseEvent) {
                            drags.incrementAndGet()
                            lastDragX.set(e.x)
                        }
                    })
                    fixture.panel.addMouseWheelListener {
                        wheels.incrementAndGet()
                        rotation.set(it.wheelRotation)
                        wheelDetail.set("rotation=${it.wheelRotation}, precise=${it.preciseWheelRotation}, units=${it.unitsToScroll}, modifiers=${it.modifiersEx}, consumed=${it.isConsumed}")
                    }
                }
                val location = onEdt { fixture.panel.locationOnScreen }
                val rowHeight = onEdt { fixture.panel.pixelHeight / fixture.panel.terminalTextBuffer.height }
                val y = location.y + startRow * rowHeight + rowHeight / 2
                val end = Point(location.x + 4 + "record-00".length * fixture.cellWidth, y)
                robot.mouseMove(end.x, end.y)
                robot.waitForIdle()
                val before = fixture.connector.writtenText()
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                buttonDown = true
                for (step in 1..8) robot.mouseMove(end.x - "record-00".length * fixture.cellWidth * step / 8, y)
                waitFor("The held native drag did not reach the beginning of the row") { lastDragX.get() <= 4 }
                robot.mouseWheel(-4)
                robot.waitForIdle()
                // macOS natural scrolling can reverse Robot's wheel direction. Keep holding and scroll toward history.
                if (rotation.get() > 0 && onEdt { fixture.panel.verticalScrollModel.value == 0 }) {
                    robot.mouseWheel(4)
                    robot.waitForIdle()
                }
                assertTrue(drags.get() > 0, "Native drag did not reach the terminal")
                assertTrue(wheels.get() > 0, "Native wheel did not reach the terminal while the button was held")
                waitFor("History did not scroll: ${wheelDetail.get()}") { onEdt { fixture.panel.verticalScrollModel.value < 0 } }
                val endRecord = onEdt { recordAt(fixture, fixture.panel.verticalScrollModel.value + startRow) }
                assertTrue(endRecord < startRecord, "The held gesture must reach earlier output")
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                buttonDown = false
                robot.waitForIdle()
                val expected = (endRecord..startRecord).joinToString("\n") { "record-%02d".format(it) }
                val selected = onEdt {
                    val selection = fixture.panel.selection ?: error("The held drag did not create a selection")
                    SelectionUtil.getSelectionText(selection, fixture.panel.terminalTextBuffer)
                }
                assertEquals(expected, selected, "The highlight must include the lines reached by scrolling")
                waitFor { onEdt { fixture.panel.isFocusOwner } }
                shortcut(robot, KeyEvent.VK_C)
                assertEquals(expected, clipboard.getData(DataFlavor.stringFlavor), "Copy must include the entire scroll-expanded selection")
                assertEquals(before, fixture.connector.writtenText(), "Selection/copy must not send mouse clicks or text to the CLI")
                shortcut(robot, KeyEvent.VK_V)
                assertTrue(fixture.connector.awaitText(before + "\u001B[200~" + expected.replace('\n', '\r') + "\u001B[201~"),
                    "Paste must deliver exactly the copied selection through bracketed paste")
            } finally {
                if (buttonDown) robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                onEdt { window?.dispose() }
                if (previous != null) clipboard.setContents(previous, null)
                if (layers == null) System.clearProperty("compose.layers.type") else System.setProperty("compose.layers.type", layers)
            }
        }
    }

    private fun recordAt(fixture: PreeditTestFixture, row: Int): Int =
        fixture.panel.terminalTextBuffer.getLine(row).text.removePrefix("record-").trim().toInt()

    private fun shortcut(robot: Robot, key: Int) {
        robot.keyPress(KeyEvent.VK_META)
        robot.keyPress(key)
        robot.keyRelease(key)
        robot.keyRelease(KeyEvent.VK_META)
        robot.waitForIdle()
    }

    private fun waitFor(message: String = "Native terminal condition timed out", read: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline && !read()) Thread.sleep(25)
        assertTrue(read(), message)
    }
}

private fun <T> onEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    var result: Result<T>? = null
    SwingUtilities.invokeAndWait { result = runCatching(block) }
    return requireNotNull(result).getOrThrow()
}
