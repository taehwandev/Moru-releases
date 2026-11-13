package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.SwingWindow
import app.thdev.moru.core.designsystem.material.prepareMoruNativeWindow
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.thdev.moru.core.designsystem.component.MoruDesktopPopupHost
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.awt.Container
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.Robot
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.Window as AwtWindow
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleState
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/** A real Compose/Swing window driven through its public accessibility actions. */
@OptIn(ExperimentalComposeUiApi::class)
internal class NativeWorkbenchTestWindow(
    width: Int,
    height: Int,
    menu: @Composable FrameWindowScope.() -> Unit = {},
    materialStyle: MoruMaterialStyle = MoruMaterialStyle.Standard,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) : AutoCloseable {
    private val testFrame = AtomicReference<AwtWindow?>()
    private val close = AtomicReference<() -> Unit>({})
    private val app = thread(name = "task-session-window", isDaemon = true) {
        application(exitProcessOnExit = false) {
            close.set(::exitApplication)
            SwingWindow(onCloseRequest = ::exitApplication, title = "Task session test", state = rememberWindowState(width = width.dp, height = height.dp), init = ::prepareMoruNativeWindow) {
                testFrame.set(window)
                menu()
                // As in Main: popovers close on Escape and on clicks into embedded native views.
                MoruTheme(darkTheme = darkTheme) {
                    MoruDesktopPopupHost(window, materialStyle) { content() }
                }
            }
        }
    }

    fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(100)
        }
        val names = onEdt { contexts().map { "${it.accessibleRole}: ${it.accessibleName}" }.take(100).joinToString("\n") }
        error("Timed out waiting for $description. Visible controls:\n$names")
    }

    fun exists(text: String, substring: Boolean = false): Boolean = onEdt {
        contexts().any { node -> matches(node, text, substring) }
    }

    fun click(text: String) {
        waitFor("$text control") { exists(text) }
        onEdt {
            val action = contexts().firstOrNull { matches(it, text, false) && it.accessibleAction != null }?.accessibleAction
                ?: error("No accessibility action for $text")
            assertTrue(action.doAccessibleAction(0), "Action rejected for $text")
        }
    }

    /** Hover a real control and assert that its native popup remains under the same anchor. */
    fun hoverWithoutMovingPopover(text: String) {
        waitFor("$text control") { exists(text) }
        Thread.sleep(200) // Let the popup's short reveal finish before taking its baseline.
        val (popup, before, target) = onEdt {
            val popup = testFrame.get()!!.ownedWindows.single { it.isShowing }
            java.awt.Desktop.getDesktop().requestForeground(true)
            popup.toFront()
            val component = contexts().first { matches(it, text, false) && it.accessibleAction != null }.accessibleComponent
            Triple(popup, popup.bounds, Rectangle(component.locationOnScreen, component.size))
        }
        assertTrue(target.width > 0 && target.height > 0, "Hover target must have visible bounds: $target")
        onEdt {
            fun inputTarget(component: java.awt.Component): java.awt.Component? =
                if (component.mouseMotionListeners.isNotEmpty()) component
                else (component as? Container)?.components?.firstNotNullOfOrNull(::inputTarget)
            val component = checkNotNull(inputTarget(popup))
            val point = java.awt.Point(target.x + target.width / 2, target.y + target.height / 2)
            val local = java.awt.Point(point).apply { SwingUtilities.convertPointFromScreen(this, component) }
            component.dispatchEvent(MouseEvent(component, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                local.x, local.y, point.x, point.y, 0, false, MouseEvent.NOBUTTON))
        }
        repeat(5) {
            Thread.sleep(80)
            assertEquals(before, onEdt { popup.bounds }, "Hovering $text must preserve native popup bounds")
        }
    }

    /** Exercise registered popup dismissal without letting global keyboard focus retarget Escape to another app. */
    fun pressEscapeInPopover() = onEdt {
        val popup = testFrame.get()!!.ownedWindows.single { it.isShowing }
        val event = KeyEvent(popup, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED)
        Toolkit.getDefaultToolkit().getAWTEventListeners(AWTEvent.KEY_EVENT_MASK).forEach { it.eventDispatched(event) }
        assertTrue(event.isConsumed, "The popup's native Escape listener must consume its dismissal")
    }

    fun selectedTab(text: String, substring: Boolean = false): Boolean = onEdt {
        contexts().any { matches(it, text, substring) && it.accessibleStateSet.contains(AccessibleState.SELECTED) }
    }

    /** A native panel consumes mouse input before Compose's enclosing clickable sees it. */
    fun clickTerminalPane(index: Int) = onEdt {
        fun panels(container: Container): List<java.awt.Component> = container.components.flatMap { child ->
            when {
                child.javaClass.simpleName == "MoruTerminalPanel" && child.isShowing -> listOf(child)
                child is Container -> panels(child)
                else -> emptyList()
            }
        }
        // Swing z-order differs from the grid's reading order.
        val panel = panels(testFrame.get()!!)
            .sortedWith(compareBy({ it.locationOnScreen.y }, { it.locationOnScreen.x }))[index]
        listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED).forEach { id ->
            panel.dispatchEvent(MouseEvent(panel, id, System.currentTimeMillis(), 0, 20, 20, 1, false, MouseEvent.BUTTON1))
        }
    }

    fun capture(name: String) {
        val bounds = onEdt {
            testFrame.get()!!.apply {
                toFront()
                java.awt.Desktop.getDesktop().requestForeground(true)
                ownedWindows.lastOrNull { it.isShowing }?.toFront()
            }.bounds
        }
        Thread.sleep(200)
        val output = File("build/ui-captures/$name.png")
        output.parentFile.mkdirs()
        ImageIO.write(Robot().createScreenCapture(bounds), "png", output)
    }

    /** Capture the actual owned surface, including backing pixels outside Compose's semantics tree. */
    fun captureOwnedSurface(name: String): java.awt.image.BufferedImage {
        val popup = onEdt {
            java.awt.Desktop.getDesktop().requestForeground(true)
            testFrame.get()!!.toFront()
            testFrame.get()!!.ownedWindows.single { it.isShowing }.apply { toFront(); requestFocus() }
        }
        waitFor("owned surface native focus") { onEdt { popup.isFocused } }
        val bounds = onEdt { popup.bounds }
        Thread.sleep(200)
        val image = Robot().createScreenCapture(bounds)
        val output = File("build/ui-captures/$name.png")
        output.parentFile.mkdirs()
        ImageIO.write(image, "png", output)
        return image
    }

    fun pressEscapeInOwnedSurface() {
        waitFor("owned surface keyboard focus") { onEdt {
            val popup = testFrame.get()!!.ownedWindows.single { it.isShowing }
            val focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            focus != null && SwingUtilities.getWindowAncestor(focus) == popup
        } }
        onEdt {
            val focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner!!
            focus.dispatchEvent(KeyEvent(focus, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED))
        }
    }

    fun clickBehindOwnedSurface() = onEdt {
        val owner = testFrame.get()!!
        owner.dispatchEvent(MouseEvent(owner, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0,
            10, owner.height - 10, 1, false, MouseEvent.BUTTON1))
    }

    private fun matches(node: AccessibleContext, text: String, substring: Boolean): Boolean =
        listOfNotNull(node.accessibleName, node.accessibleDescription).any {
            if (substring) it.contains(text)
            else it == text || (node.accessibleRole == AccessibleRole.PAGE_TAB && it.startsWith("$text, "))
        }

    private fun contexts(): List<AccessibleContext> {
        val result = mutableListOf<AccessibleContext>()
        fun visit(node: AccessibleContext) {
            result += node
            repeat(node.accessibleChildrenCount) { index -> node.getAccessibleChild(index)?.accessibleContext?.let(::visit) }
        }
        val frame = testFrame.get() ?: return result
        // Open popovers are owned windows drawn over the frame, so their controls come first, as the user sees them.
        frame.ownedWindows.filter { it.isShowing }.forEach { popup -> popup.accessibleContext?.let(::visit) }
        frame.accessibleContext?.let(::visit)
        return result
    }

    private fun <T> onEdt(read: () -> T): T {
        val result = AtomicReference<Result<T>>()
        SwingUtilities.invokeAndWait { result.set(runCatching(read)) }
        return result.get().getOrThrow()
    }

    override fun close() {
        close.get().invoke()
        app.join(5_000)
    }
}
