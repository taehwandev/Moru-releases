package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import app.thdev.moru.app.i18n.AppStrings
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities

/** Renders the current app commands and their platform shortcuts on the real desktop window. */
@Composable
internal fun FrameWindowScope.MoruCommandMenuBar(strings: AppStrings, commands: MoruCommands) {
    val macOs = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)
    val entries = rememberUpdatedState(appMenu(strings, commands).flatMap { it.entries })
    DisposableEffect(window, macOs) {
        val focus = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val dispatcher = AppShortcutDispatcher({ entries.value }, macOs) { component ->
            val sourceWindow = (component as? Window) ?: SwingUtilities.getWindowAncestor(component)
            generateSequence(sourceWindow) { it.owner }.any { it == window }
        }
        val listener = object : WindowAdapter() {
            override fun windowLostFocus(event: WindowEvent) = dispatcher.reset()
        }
        focus.addKeyEventDispatcher(dispatcher)
        window.addWindowFocusListener(listener)
        onDispose {
            focus.removeKeyEventDispatcher(dispatcher)
            window.removeWindowFocusListener(listener)
        }
    }
    MenuBar {
        appMenu(strings, commands).forEach { group ->
            Menu(group.label) {
                group.entries.forEach { entry ->
                    Item(
                        text = entry.label,
                        onClick = entry.onClick,
                        enabled = entry.enabled,
                        shortcut = entry.key?.let { key -> KeyShortcut(key, ctrl = !macOs, meta = macOs, alt = entry.alt, shift = entry.shift) },
                    )
                }
            }
        }
    }
}
