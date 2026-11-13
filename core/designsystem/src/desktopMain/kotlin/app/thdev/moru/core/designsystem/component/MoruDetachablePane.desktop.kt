package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
actual fun MoruDetachablePane(
    detached: Boolean,
    onRestore: () -> Unit,
    title: String,
    modifier: Modifier,
    placeholder: @Composable (Modifier) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val owner = LocalMoruPopupOwner.current
    // Offscreen previews have no native window. Production installs MoruDesktopPopupHost at its root.
    if (owner == null) {
        check(!detached) { "A detachable pane needs MoruDesktopPopupHost in its native window" }
        Box(modifier) { content(Modifier.fillMaxSize()) }
        return
    }
    val host = remember(owner) { DesktopPaneHost(owner) }
    val latestContent by rememberUpdatedState(content)
    val latestColors by rememberUpdatedState(MoruTheme.colors)
    val latestTypography by rememberUpdatedState(MoruTheme.typography)
    val latestMaterialStyle by rememberUpdatedState(MoruTheme.materialStyle)
    val latestLanguage by rememberUpdatedState(LocalUiLanguage.current)
    DisposableEffect(host) {
        host.panel.setContent {
            // Carry product context, while the panel keeps its own input, focus, density and popup scene.
            MoruTheme(colors = latestColors, typography = latestTypography, materialStyle = latestMaterialStyle) {
                CompositionLocalProvider(LocalUiLanguage provides latestLanguage) {
                    MoruDesktopPopupHost(host.windowOwner) {
                        latestContent(Modifier.requiredSize(host.size.width.dp, host.size.height.dp))
                    }
                }
            }
        }
        onDispose { host.dispose() }
    }
    Box(modifier) {
        if (detached) placeholder(Modifier.fillMaxSize())
        else SwingPanel(factory = { host.dock }, background = Color.Transparent, modifier = Modifier.fillMaxSize())
    }
    SideEffect { host.move(detached, title, onRestore) }
}
