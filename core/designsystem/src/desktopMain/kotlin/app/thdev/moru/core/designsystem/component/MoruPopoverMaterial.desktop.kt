package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import app.thdev.moru.core.designsystem.theme.LocalMoruNativeMaterial
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Window-layer popovers draw the theme surface without embedding a Swing attachment probe. */
@Composable
internal actual fun MoruPopoverMaterial(content: @Composable () -> Unit) {
    val color = MoruTheme.colors.primary
    val indication = remember(color) { MoruPopoverIndication(color) }
    CompositionLocalProvider(
        LocalMoruNativeMaterial provides false,
        LocalIndication provides indication,
        content = content,
    )
}
