package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import java.awt.Component
import java.awt.Container
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Actual JediTerm key routing, matching and painting, without changing OS focus or starting a user process. */
class TerminalFindTest {
    @Test
    fun commandFFocusesAndSelectsTheQueryAgainWithoutSendingItToTheTerminal() {
        PreeditTestFixture().use { fixture ->
            onFindEdt {
                fixture.panel.processKeyEvent(commandF(fixture.panel))
                val controller = fixture.widget.findController
                assertTrue(controller.isOpen)
                listOf(
                    KeyEvent(fixture.panel, KeyEvent.KEY_TYPED, 0, InputEvent.META_DOWN_MASK, KeyEvent.VK_UNDEFINED, 'f'),
                    KeyEvent(fixture.panel, KeyEvent.KEY_RELEASED, 0, InputEvent.META_DOWN_MASK, KeyEvent.VK_F, 'f'),
                ).forEach {
                    fixture.panel.processKeyEvent(it)
                    assertTrue(it.isConsumed)
                }
                val query = controller.bar.queryField
                query.text = "needle"
                query.select(2, 2)
                val event = commandF(query)
                query.keyListeners.forEach { it.keyPressed(event) }
                assertTrue(event.isConsumed)
                assertEquals("needle", query.selectedText)
                assertSame(query, controller.keyboardTarget)
                val before = fixture.widget.focusRequests.get()
                fixture.widget.requestFocusInWindow()
                assertEquals(before + 1, fixture.widget.focusRequests.get(), "A retained-pane focus request must not ask the PTY panel for focus")
            }
            assertFalse(fixture.connector.hasWriteWithin(100), "Find shortcuts and field editing must remain local")
        }
    }

    @Test
    fun editingRevealsTheFirstScrollbackHitAndEnterAndShiftEnterWrap() {
        PreeditTestFixture().use { fixture ->
            fixture.output("\u001b[2J\u001b[H")
            repeat(35) { line -> fixture.output("${if (line == 1 || line == 20) "needle" else "row"} $line\r\n") }
            fixture.find("NEEDLE", 2)
            onFindEdt {
                val controller = fixture.widget.findController
                val result = assertNotNull(fixture.panel.findResult)
                assertEquals(2, result.items.size)
                assertEquals("1 / 2", controller.bar.resultLabel.text)
                assertTrue(assertNotNull(fixture.panel.selection).start.y < 0, "Typing must immediately reveal the retained scrollback hit")
                assertTrue(fixture.panel.verticalScrollModel.value < 0)
                val origin = fixture.panel.verticalScrollModel.value
                fixture.paint()
                assertEquals(origin, fixture.panel.verticalScrollModel.value, "The next native paint must not apply old scroll deltas to the chosen result")
                val matchRow = assertNotNull(fixture.panel.selection).start.y
                assertTrue(matchRow in origin until origin + fixture.panel.verticalScrollModel.extent, "The retained match must be inside the actual viewport")
                queryKey(controller.bar.queryField, KeyEvent.VK_ENTER)
                assertEquals("2 / 2", controller.bar.resultLabel.text)
                queryKey(controller.bar.queryField, KeyEvent.VK_ENTER)
                assertEquals("1 / 2", controller.bar.resultLabel.text)
                queryKey(controller.bar.queryField, KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)
                assertEquals("2 / 2", controller.bar.resultLabel.text)
                assertSame(controller.bar.queryField, controller.keyboardTarget, "Navigation must keep the search field as the keyboard target")
            }
            assertFalse(fixture.connector.hasWriteWithin(100), "Enter in Find must not submit to the CLI")
        }
    }

    @Test
    fun aQueryWithNoMatchesClearsThePreviousCountAndEmptyQueryClearsHighlights() {
        PreeditTestFixture().use { fixture ->
            fixture.output("\u001b[2J\u001b[Hneedle")
            fixture.find("needle", 1)
            onFindEdt {
                assertEquals("1 / 1", fixture.widget.findController.bar.resultLabel.text)
            }
            fixture.find("missing", 0)
            onFindEdt {
                val controller = fixture.widget.findController
                assertEquals("0 results", controller.bar.resultLabel.text)
                assertFalse(controller.bar.previousButton.isEnabled)
                assertFalse(controller.bar.nextButton.isEnabled)
                assertNull(fixture.panel.selection)
            }
            fixture.output(" more output")
            fixture.refreshFind(0)
            onFindEdt {
                val controller = fixture.widget.findController
                // Refreshing an empty result must not call selectedItem(), which asserts in JediTerm.
                assertEquals("0 results", controller.bar.resultLabel.text)
                controller.bar.queryField.text = ""
                assertNull(fixture.panel.findResult)
                assertEquals("", controller.bar.resultLabel.text)
            }
        }
    }

    @Test
    fun outputRefreshKeepsTheChosenMatchAndNavigationSeesNewOutput() {
        PreeditTestFixture().use { fixture ->
            fixture.output("\u001b[2J\u001b[Hneedle one\r\nneedle two")
            fixture.find("needle", 2)
            onFindEdt {
                val controller = fixture.widget.findController
                queryKey(controller.bar.queryField, KeyEvent.VK_ENTER)
                assertEquals("2 / 2", controller.bar.resultLabel.text)
            }
            fixture.output("\r\nneedle three")
            fixture.refreshFind(3)
            onFindEdt {
                val controller = fixture.widget.findController
                assertEquals("2 / 3", controller.bar.resultLabel.text)
            }
            fixture.output("\r\nneedle four")
            onFindEdt {
                val controller = fixture.widget.findController
                queryKey(controller.bar.queryField, KeyEvent.VK_ENTER) // Refresh and select under the same model lock.
                assertEquals("3 / 4", controller.bar.resultLabel.text)
                assertEquals(2, assertNotNull(fixture.panel.selection).start.y)
            }
        }
    }

    @Test
    fun escapeFromFindConsumesTheEventTailAfterFocusReturnsToTheTerminal() {
        PreeditTestFixture().use { fixture ->
            onFindEdt {
                fixture.panel.processKeyEvent(commandF(fixture.panel))
                val controller = fixture.widget.findController
                queryKey(controller.bar.queryField, KeyEvent.VK_ESCAPE)
                assertFalse(controller.isOpen)
                assertNull(fixture.panel.findResult)
                listOf(
                    KeyEvent(fixture.panel, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, '\u001b'),
                    KeyEvent(fixture.panel, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_ESCAPE, '\u001b'),
                ).forEach {
                    fixture.panel.processKeyEvent(it)
                    assertTrue(it.isConsumed)
                }
                assertSame(fixture.panel, controller.keyboardTarget)
            }
            assertFalse(fixture.connector.hasWriteWithin(100), "Closing Find must not send Escape to the running agent")
        }
    }

    @Test
    fun escapeAlsoClosesFindWhenTheUserHasClickedBackIntoTheTerminal() {
        PreeditTestFixture().use { fixture ->
            onFindEdt {
                fixture.widget.findController.open()
                val event = KeyEvent(fixture.panel, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, '\u001b')
                fixture.panel.processKeyEvent(event)
                assertTrue(event.isConsumed)
                assertFalse(fixture.widget.findController.isOpen)
            }
            assertFalse(fixture.connector.hasWriteWithin(100))
        }
    }

    @Test
    fun nativeFindPaintsWithReachableControlsInLightDarkAndCompactPanes() {
        listOf(false, true).forEach { dark ->
            listOf(640, 240).forEach { width ->
                PreeditTestFixture(dark = dark).use { fixture ->
                    fixture.output("\u001b[2J\u001b[Hneedle in screen")
                    onFindEdt {
                        val widget = fixture.widget
                        val palette = TerminalPalette.from(if (dark) darkCkColors else lightCkColors)
                        palette.applyTo(widget)
                        widget.findController.open()
                        widget.setSize(width, 220)
                        layoutFindTree(widget)
                    }
                    fixture.find("needle", 1)
                    onFindEdt {
                        val widget = fixture.widget
                        layoutFindTree(widget)
                        val bar = widget.findController.bar
                        assertEquals("1 / 1", bar.resultLabel.text, "Capture must represent completed search")
                        assertTrue(bar.width <= width && bar.x >= 0)
                        listOf<Component>(bar.queryField, bar.resultLabel, bar.previousButton, bar.nextButton, bar.closeButton).forEach { control ->
                            val origin = SwingUtilities.convertPoint(control, 0, 0, widget)
                            assertTrue(control.width > 0 && control.height > 0)
                            assertTrue(origin.x >= 0 && origin.x + control.width <= width, "Find control ${control.name ?: control.javaClass.simpleName} must fit $width px")
                        }
                        assertTrue(bar.queryField.width >= 40, "The query must remain editable in a compact split")
                        assertEquals(fixture.panel.background, bar.queryField.background)
                        assertEquals(fixture.panel.foreground, bar.queryField.foreground)
                        val image = BufferedImage(width, 220, BufferedImage.TYPE_INT_RGB)
                        image.createGraphics().useFindGraphics { widget.paint(it) }
                        listOf(bar.previousButton, bar.nextButton, bar.closeButton).forEach { button ->
                            val point = SwingUtilities.convertPoint(button, 0, 0, widget)
                            val fill = image.getRGB(point.x + 2, point.y + button.height / 2)
                            val glyphContrast = (5 until button.width - 5).maxOf { x ->
                                (5 until button.height - 5).maxOf { y ->
                                    contrastRatio(fill, image.getRGB(point.x + x, point.y + y))
                                }
                            }
                            assertTrue(glyphContrast >= 3.0, "${button.toolTipText} must paint a readable glyph against its actual ${if (dark) "dark" else "light"} fill, got $glyphContrast")
                        }
                        val query = bar.queryField
                        val origin = SwingUtilities.convertPoint(query, 0, 0, widget)
                        val background = query.background.rgb
                        assertTrue((origin.x + 4 until origin.x + query.width - 4).any { x ->
                            (origin.y + 4 until origin.y + query.height - 4).any { y -> image.getRGB(x, y) != background }
                        }, "The search query must paint visible text")
                        run {
                            val directory = System.getenv("MORU_TERMINAL_FIND_CAPTURE_DIR")?.takeIf { it.isNotBlank() }
                                ?: "build/reports/terminal-find"
                            val file = File(directory, "terminal-find-${if (dark) "dark" else "light"}-$width.png")
                            file.parentFile.mkdirs()
                            assertTrue(ImageIO.write(image, "png", file))
                        }
                    }
                }
            }
        }
    }
}

private fun commandF(source: Component) = KeyEvent(source, KeyEvent.KEY_PRESSED, 0, InputEvent.META_DOWN_MASK, KeyEvent.VK_F, 'f')

private fun PreeditTestFixture.find(query: String, expectedCount: Int) {
    onFindEdt {
        panel.processKeyEvent(commandF(panel))
        widget.findController.bar.queryField.text = query
    }
    refreshFind(expectedCount)
}

/** tryLock deliberately yields to terminal output; retry outside EDT rather than recording a pending UI as a result. */
private fun PreeditTestFixture.refreshFind(expectedCount: Int) {
    val deadline = System.nanoTime() + 2_000_000_000L
    while (System.nanoTime() < deadline) {
        val ready = onFindEdt {
            val controller = widget.findController
            controller.refreshNow()
            val count = panel.findResult?.items?.size ?: 0
            count == expectedCount && controller.bar.resultLabel.text != "…" &&
                controller.bar.nextButton.isEnabled == (expectedCount > 0)
        }
        if (ready) return
        Thread.sleep(10)
    }
    error(onFindEdt { "Timed out finding $expectedCount matches; label=${widget.findController.bar.resultLabel.text}, count=${panel.findResult?.items?.size}" })
}

private fun contrastRatio(first: Int, second: Int): Double {
    fun luminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val value = (rgb shr shift and 255) / 255.0
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        return channel(16) * 0.2126 + channel(8) * 0.7152 + channel(0) * 0.0722
    }
    val a = luminance(first)
    val b = luminance(second)
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}

private fun queryKey(field: javax.swing.JTextField, code: Int, modifiers: Int = 0) {
    val event = KeyEvent(field, KeyEvent.KEY_PRESSED, 0, modifiers, code, KeyEvent.CHAR_UNDEFINED)
    field.keyListeners.forEach { it.keyPressed(event) }
    assertTrue(event.isConsumed)
}

private fun layoutFindTree(component: Component) {
    if (component is Container) {
        component.doLayout()
        component.components.forEach(::layoutFindTree)
    }
}

private inline fun java.awt.Graphics2D.useFindGraphics(block: (java.awt.Graphics2D) -> Unit) {
    try { block(this) } finally { dispose() }
}

private fun <T> onFindEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    val result = AtomicReference<Result<T>>()
    SwingUtilities.invokeAndWait { result.set(runCatching(block)) }
    return result.get().getOrThrow()
}
