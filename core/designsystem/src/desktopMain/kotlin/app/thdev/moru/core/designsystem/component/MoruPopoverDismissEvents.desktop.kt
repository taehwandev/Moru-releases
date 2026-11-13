package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import java.awt.AWTEvent
import java.awt.Component
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

@Composable
@OptIn(InternalComposeUiApi::class)
internal actual fun MoruPopoverDismissEvents(
    onDismiss: () -> Unit,
    onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean,
) {
    val owner = LocalMoruPopupOwner.current ?: return
    val latestDismiss = rememberUpdatedState(onDismiss)
    val latestKeyHandler = rememberUpdatedState(onPreviewKeyEvent)
    DisposableEffect(owner) {
        val toolkit = Toolkit.getDefaultToolkit()
        val listener = AWTEventListener { event ->
            val popup = owner.ownedWindows.lastOrNull { it.isShowing }
            val source = event.source as? Component
            if (popup != null && source != null) {
                val sourceWindow = SwingUtilities.getWindowAncestor(source)
                if (event is MouseEvent && event.id == MouseEvent.MOUSE_PRESSED && sourceWindow != popup) {
                    latestDismiss.value()
                } else if (event is KeyEvent && event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_ESCAPE && (sourceWindow == popup || sourceWindow == owner)) {
                    latestDismiss.value()
                    event.consume()
                }
            }
        }
        toolkit.addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK or AWTEvent.KEY_EVENT_MASK)
        // Embedded Swing terminals can retain focus when a native popup opens. Intercept before
        // either the terminal or Compose receives the event; consumption prevents duplicate starts.
        val focus = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val pressed = mutableSetOf<Int>()
        val dispatcher = KeyEventDispatcher { event ->
            val popup = owner.ownedWindows.lastOrNull { it.isShowing }
            val sourceWindow = SwingUtilities.getWindowAncestor(event.component)
            val belongsToPopover = popup != null && (sourceWindow == popup || sourceWindow == owner)
            val handled = !event.isConsumed && belongsToPopover &&
                (if (event.id == KeyEvent.KEY_TYPED) pressed.isNotEmpty()
                else latestKeyHandler.value(ComposeKeyEvent(
                    key = Key(event.keyCode, event.keyLocation.takeUnless { it == KeyEvent.KEY_LOCATION_UNKNOWN }
                        ?: KeyEvent.KEY_LOCATION_STANDARD),
                    type = if (event.id == KeyEvent.KEY_PRESSED) KeyEventType.KeyDown else KeyEventType.KeyUp,
                    isAltPressed = event.isAltDown,
                    isCtrlPressed = event.isControlDown,
                    isMetaPressed = event.isMetaDown,
                    isShiftPressed = event.isShiftDown,
                    nativeEvent = event,
                )))
            if (event.id == KeyEvent.KEY_RELEASED) pressed.remove(event.keyCode)
            if (handled) {
                if (event.id == KeyEvent.KEY_PRESSED) pressed += event.keyCode
                event.consume()
                true
            } else false
        }
        focus.addKeyEventDispatcher(dispatcher)
        onDispose {
            focus.removeKeyEventDispatcher(dispatcher)
            toolkit.removeAWTEventListener(listener)
        }
    }
}
