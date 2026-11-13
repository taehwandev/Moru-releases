package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Robot
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingUtilities
import javax.swing.TransferHandler
import org.junit.Assume.assumeFalse
import kotlin.test.Test
import kotlin.test.assertTrue

/** A file dragged with the real pointer onto a terminal inside the Compose window is pasted into that terminal. */
class TerminalNativeDropOsTest {
    @Test
    fun aFileDraggedOntoTheTerminalIsPasted() {
        assumeFalse("A native drag requires a display", GraphicsEnvironment.isHeadless())
        val previous = System.getProperty("compose.layers.type")
        System.setProperty("compose.layers.type", "WINDOW")
        val file = File("/tmp/moru drop.png")
        val source = JLabel("drag me").apply {
            transferHandler = object : TransferHandler() {
                override fun getSourceActions(c: JComponent) = COPY
                override fun createTransferable(c: JComponent): Transferable = object : Transferable {
                    override fun getTransferDataFlavors() = arrayOf(DataFlavor.javaFileListFlavor)
                    override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.javaFileListFlavor
                    override fun getTransferData(flavor: DataFlavor): Any = listOf(file)
                }
            }
            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) = transferHandler.exportAsDrag(this@apply, e, TransferHandler.COPY)
            })
        }
        PreeditTestFixture().use { fixture ->
            var window: ComposeWindow? = null
            var sourceFrame: JFrame? = null
            try {
                val robot = Robot().apply { autoDelay = 30 }
                onEdt {
                    window = ComposeWindow().apply {
                        setSize(560, 320)
                        setLocation(560, 80)
                        setContent { SwingPanel(factory = { fixture.widget }, modifier = Modifier.fillMaxSize()) }
                        isVisible = true
                    }
                    sourceFrame = JFrame("drop source").apply {
                        contentPane.add(source)
                        setSize(200, 120)
                        setLocation(80, 80)
                        isVisible = true
                        toFront()
                    }
                }
                waitFor { onEdt { fixture.panel.isShowing && fixture.panel.width > 100 && source.isShowing } }
                val from = onEdt { source.locationOnScreen.let { Point(it.x + source.width / 2, it.y + source.height / 2) } }
                val to = onEdt { fixture.panel.locationOnScreen.let { Point(it.x + fixture.panel.width / 2, it.y + 40) } }
                robot.mouseMove(from.x, from.y)
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                for (step in 1..30) {
                    robot.mouseMove(from.x + (to.x - from.x) * step / 30, from.y + (to.y - from.y) * step / 30)
                }
                robot.delay(300)
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                assertTrue(fixture.connector.awaitText("/tmp/moru\\ drop.png "), fixture.connector.writtenText())
            } finally {
                onEdt {
                    window?.dispose()
                    sourceFrame?.dispose()
                }
                if (previous == null) System.clearProperty("compose.layers.type") else System.setProperty("compose.layers.type", previous)
            }
        }
    }

    private fun waitFor(read: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline && !read()) Thread.sleep(50)
    }

    private fun <T> onEdt(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }
}
