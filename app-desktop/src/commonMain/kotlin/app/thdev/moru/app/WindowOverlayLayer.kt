package app.thdev.moru.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.component.MoruDetachablePane

/** Uses the existing native pane host to draw and receive input above embedded Swing terminals. */
@Composable
internal fun WindowOverlayLayer(modifier: Modifier, content: @Composable () -> Unit) {
    MoruDetachablePane(
        detached = false,
        onRestore = {},
        title = "",
        modifier = modifier,
        placeholder = {},
    ) { bounds -> Box(bounds.fillMaxSize()) { content() } }
}
