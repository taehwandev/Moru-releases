package app.thdev.moru.app

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/** The About window; Escape closes it. */
@Composable
internal fun AboutMoruDialog(currentVersion: String?, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    AboutMoruWindow(onDismiss = onDismiss) {
        Box(Modifier.onPreviewKeyEvent { event ->
            if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) { onDismiss(); true } else false
        }.focusRequester(focus).focusable()) {
            AboutMoruContent(
                versionLabel = appVersionLabel(currentVersion),
                onClose = onDismiss,
            )
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}
