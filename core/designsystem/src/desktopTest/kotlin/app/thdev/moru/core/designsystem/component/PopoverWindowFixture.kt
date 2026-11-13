package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.material.prepareMoruNativeWindow
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Desktop
import java.awt.KeyboardFocusManager
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

internal class PopoverWindowFixture(dark: Boolean) : AutoCloseable {
    var expanded by mutableStateOf(false)
    private var edge by mutableStateOf(Alignment.TopStart)
    private var oversized by mutableStateOf(false)
    var dismissals = 0
    var shortcuts = 0
    private var windowLevel: AutoCloseable? = null
    val owner: ComposeWindow = onEdt {
        ComposeWindow().apply {
            title = "Popover geometry test"
            setSize(320, 240)
            setLocation(60, 80)
            prepareMoruNativeWindow(this)
            windowLevel = installPopoverFixtureWindowLevel(this)
            val window = this
            setContent {
                MoruTheme(darkTheme = dark, materialStyle = MoruMaterialStyle.Apple) {
                    MoruDesktopPopupHost(window) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.align(edge).size(0.dp)) {
                                MoruPopover(expanded, { dismissals++; expanded = false },
                                    modifier = Modifier.width(if (oversized) 900.dp else 180.dp),
                                    onPreviewKeyEvent = {
                                        if (it.key == Key.One) {
                                            if (it.type == KeyEventType.KeyUp) shortcuts++
                                            true
                                        } else false
                                    },
                                ) {
                                    if (oversized) {
                                        Column(Modifier.padding(6.dp).fillMaxWidth()) {
                                            repeat(30) { index -> MoruText("Long content $index", modifier = Modifier.height(30.dp)) }
                                        }
                                    } else {
                                        Box(Modifier.padding(6.dp).fillMaxWidth().height(30.dp)) {
                                            MoruText("Compact content")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            isVisible = true
            toFront()
            requestFocus()
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) desktop.requestForeground(true)
            }
        }
    }

    fun open(at: Alignment, oversized: Boolean = false) {
        onEdt {
            owner.toFront()
            owner.requestFocus()
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) desktop.requestForeground(true)
            }
        }
        // Opening from a real anchor happens after the owner has native focus. Do not race initial
        // application activation by showing its focusable child in the same EDT turn as requestFocus().
        waitFor("the owner to receive native focus before expanding") { onEdt { owner.isFocused } }
        onEdt {
            edge = at
            this.oversized = oversized
            expanded = true
        }
    }

    fun popup(): Window? = owner.ownedWindows.singleOrNull { it.isShowing && it.width > 0 && it.height > 0 }
    fun contentBounds(): Rectangle = Rectangle(owner.contentPane.locationOnScreen, owner.contentPane.size)

    fun hasKeyboardTarget(): Boolean = keyboardTarget() != null

    private fun keyboardTarget(): Component? {
        val popup = popup() ?: return null
        val focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner ?: return null
        val window = (focus as? Window) ?: SwingUtilities.getWindowAncestor(focus)
        // Production forwards popup shortcuts even when an embedded owner view retains native focus.
        return focus.takeIf { it.isShowing && (window == owner || window == popup) && window.isFocused }
    }

    fun postKey(code: Int, character: Char) {
        waitFor("an active keyboard target to receive key $code exactly once") {
            onEdt {
                // Native focus can change between EDT turns. Validate and send the one requested pair
                // in the same turn; an unavailable target causes no event and is safe to await again.
                val target = keyboardTarget()
                if (target == null) false else {
                    listOf(KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED).forEach { id ->
                        target.dispatchEvent(KeyEvent(target, id, System.currentTimeMillis(), 0, code, character))
                    }
                    true
                }
            }
        }
    }

    fun hide() {
        onEdt { expanded = false }
        waitFor("popup closes") { onEdt { popup() == null } }
    }

    override fun close() {
        onEdt {
            owner.dispose()
            windowLevel?.close()
        }
    }

    fun waitFor(what: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(25)
        }
        error("Timed out waiting for $what: " + onEdt {
            val focus = KeyboardFocusManager.getCurrentKeyboardFocusManager()
            "owner=${owner.bounds}, ownerFocused=${owner.isFocused}, content=${contentBounds()}, " +
                "focusOwner=${focus.focusOwner}, focusedWindow=${focus.focusedWindow}, " +
                "popups=${owner.ownedWindows.map { "${it.bounds} showing=${it.isShowing} focused=${it.isFocused}" }}"
        })
    }

    fun <T> onEdt(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }
}

/** Match the production popup window level before its first frame, including macOS focus restoration. */
internal fun installPopoverFixtureWindowLevel(owner: ComposeWindow): AutoCloseable {
    val toolkit = Toolkit.getDefaultToolkit()
    val listener = java.awt.event.AWTEventListener { event ->
        if (event is WindowEvent && event.id == WindowEvent.WINDOW_OPENED && event.window.owner == owner) {
            val popup = event.window
            if (popup.isAlwaysOnTop) {
                popup.isAlwaysOnTop = false
                popup.toFront()
                if (popup.isFocusableWindow && popup.isAutoRequestFocus) popup.requestFocus()
            }
        }
    }
    toolkit.addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK)
    return AutoCloseable { toolkit.removeAWTEventListener(listener) }
}
