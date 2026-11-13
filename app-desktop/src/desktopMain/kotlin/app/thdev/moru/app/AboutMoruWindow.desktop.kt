package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.DialogModalityType
import androidx.compose.ui.window.rememberDialogState
import app.thdev.moru.core.designsystem.component.LocalMoruPopupOwner
import app.thdev.moru.core.designsystem.theme.LocalMoruNativeMaterial
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/** A fixed owned dialog avoids scene-layer resizing and paints every pixel without the parent's backdrop. */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun AboutMoruWindow(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val owner = LocalMoruPopupOwner.current
    if (owner == null) {
        // Windowless Compose fixtures keep the content in their test scene.
        Dialog(onDismissRequest = onDismiss) {
            CompositionLocalProvider(LocalMoruNativeMaterial provides false, content = content)
        }
        return
    }
    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(size = DpSize(384.dp, 600.dp)),
        title = "About Moru",
        resizable = false,
        modalityType = DialogModalityType.Modeless,
    ) {
        val latestDismiss = rememberUpdatedState(onDismiss)
        DisposableEffect(window, owner) {
            val listener = AWTEventListener { event ->
                val source = event.source as? Component
                if (event is MouseEvent && event.id == MouseEvent.MOUSE_PRESSED && source != null &&
                    (source == owner || SwingUtilities.getWindowAncestor(source) == owner)) latestDismiss.value()
            }
            val toolkit = Toolkit.getDefaultToolkit()
            toolkit.addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK)
            onDispose { toolkit.removeAWTEventListener(listener) }
        }
        CompositionLocalProvider(LocalMoruNativeMaterial provides false) {
            Box(Modifier.fillMaxSize().background(MoruTheme.colors.background).padding(12.dp),
                contentAlignment = Alignment.Center) { content() }
        }
    }
}
