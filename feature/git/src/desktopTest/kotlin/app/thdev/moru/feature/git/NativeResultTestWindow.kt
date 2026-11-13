package app.thdev.moru.feature.git

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.thdev.moru.core.designsystem.component.MoruDesktopPopupHost
import java.awt.Window as AwtWindow
import java.awt.Component
import java.awt.Container
import java.awt.event.WindowEvent
import java.util.concurrent.atomic.AtomicReference
import javax.accessibility.AccessibleContext
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.test.assertTrue

/** Only the fixture's own windows receive accessibility actions. */
internal class NativeResultTestWindow(content: @Composable () -> Unit) : AutoCloseable {
    private val close = AtomicReference<() -> Unit>({})
    private val failure = AtomicReference<Throwable?>()
    private val app = thread(name = "result-placement-test", isDaemon = true) {
        runCatching {
            application(exitProcessOnExit = false) {
                close.set(::exitApplication)
                Window(onCloseRequest = ::exitApplication, title = MAIN_TITLE,
                    state = rememberWindowState(width = 960.dp, height = 520.dp)) {
                    MoruDesktopPopupHost(window) { content() }
                }
            }
        }.onFailure(failure::set)
    }

    fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            failure.get()?.let { throw it }
            if (condition()) return
            Thread.sleep(50)
        }
        val controls = onEdt {
            AwtWindow.getWindows().filterIsInstance<javax.swing.JFrame>().filter { it.isShowing }.joinToString("\n") { frame ->
                frame.title + ": " + contexts(frame.title).joinToString("\n") {
                    "${it.accessibleRole}: ${it.accessibleName} / ${it.accessibleDescription} editable=${it.accessibleEditableText != null}"
                }
            }
        }
        error("Timed out waiting for $description. Controls: $controls")
    }

    fun visible(title: String): Boolean = onEdt { frame(title)?.isShowing == true }
    fun exists(title: String, text: String): Boolean = onEdt { contexts(title).any { matches(it, text) } }
    fun click(title: String, text: String) {
        waitFor("$title: $text") { exists(title, text) }
        onEdt {
            val action = contexts(title).firstOrNull { matches(it, text) && it.accessibleAction != null }?.accessibleAction
                ?: error("No accessibility action for $text in $title")
            assertTrue(action.doAccessibleAction(0), "Rejected $text in $title")
        }
    }

    fun closeNative(title: String) = onEdt {
        frame(title)!!.dispatchEvent(WindowEvent(frame(title), WindowEvent.WINDOW_CLOSING))
    }

    fun runOnEdt(action: () -> Unit) = onEdt(action)
    fun resizeMain(width: Int, height: Int) = onEdt { frame(MAIN_TITLE)!!.setSize(width, height) }
    fun paneWidth(title: String): Int = onEdt {
        fun find(component: Component): ComposePanel? =
            if (component is ComposePanel) component else (component as? Container)?.components?.firstNotNullOfOrNull(::find)
        frame(title)?.let(::find)?.width ?: 0
    }

    fun replaceEditableText(title: String, text: String, value: String) {
        waitFor("$title editable $text") {
            onEdt { contexts(title).any { node ->
                node.accessibleEditableText != null && listOfNotNull(node.accessibleName, node.accessibleDescription).any { it.contains(text) }
            } }
        }
        onEdt {
            contexts(title).first { node ->
                node.accessibleEditableText != null && listOfNotNull(node.accessibleName, node.accessibleDescription).any { it.contains(text) }
            }.accessibleEditableText.setTextContents(value)
        }
    }
    fun editableValue(title: String, text: String): String? = onEdt {
        val editable = contexts(title).firstOrNull { node ->
            node.accessibleEditableText != null && listOfNotNull(node.accessibleName, node.accessibleDescription).any { it.contains(text) }
        }?.accessibleEditableText ?: return@onEdt null
        editable.getTextRange(0, editable.charCount)
    }

    private fun frame(title: String): javax.swing.JFrame? = AwtWindow.getWindows().filterIsInstance<javax.swing.JFrame>()
        .firstOrNull { it.title == title && it.isDisplayable }
    private fun matches(node: AccessibleContext, text: String) =
        node.accessibleName == text || node.accessibleDescription == text
    private fun contexts(title: String): List<AccessibleContext> {
        val nodes = mutableListOf<AccessibleContext>()
        fun visit(node: AccessibleContext) {
            nodes += node
            repeat(node.accessibleChildrenCount) { node.getAccessibleChild(it)?.accessibleContext?.let(::visit) }
        }
        frame(title)?.accessibleContext?.let(::visit)
        return nodes
    }

    private fun <T> onEdt(read: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return read()
        val value = AtomicReference<Result<T>>()
        SwingUtilities.invokeAndWait { value.set(runCatching(read)) }
        return value.get().getOrThrow()
    }

    override fun close() {
        close.get().invoke()
        app.join(5_000)
        failure.get()?.let { throw it }
        assertTrue(!app.isAlive, "Result fixture must stop")
    }

    companion object { const val MAIN_TITLE = "Result layout fixture" }
}
