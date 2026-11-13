package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.awt.Window
import app.thdev.moru.core.designsystem.theme.LocalMoruNativeMaterial
import app.thdev.moru.core.designsystem.theme.LocalMoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.material.rememberMacBackdrop

/** Explicit desktop owner for native popup input; no dependency on private Compose window APIs. */
@Composable
fun MoruDesktopPopupHost(
    window: Window,
    materialStyle: MoruMaterialStyle = MoruTheme.materialStyle,
    darkTheme: Boolean = !MoruTheme.colors.isLight,
    content: @Composable () -> Unit,
) {
    val nativeMaterial = rememberMacBackdrop(
        window.takeIf { materialStyle == MoruMaterialStyle.Apple }, role = 0, dark = if (darkTheme) 1 else 0,
    )
    CompositionLocalProvider(
        LocalMoruPopupOwner provides window,
        LocalMoruNativeMaterial provides nativeMaterial,
        LocalMoruMaterialStyle provides materialStyle,
    ) {
        // macOS passes mouse-down through fully transparent renderer pixels. A single
        // alpha level keeps empty chrome interactive while AppKit supplies the material.
        Box(
            Modifier.background(if (nativeMaterial) Color.Black.copy(alpha = 1f / 255f) else Color.Transparent),
            propagateMinConstraints = true,
        ) { content() }
    }
}
