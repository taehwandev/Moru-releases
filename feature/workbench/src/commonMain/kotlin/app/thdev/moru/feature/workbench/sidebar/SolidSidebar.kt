package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.thdev.moru.core.designsystem.theme.LocalMoruNativeMaterial
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Keep navigation opaque while inheriting the user's palette and text settings. */
@Composable
internal fun SolidSidebar(content: @Composable () -> Unit) {
    MoruTheme(
        colors = MoruTheme.colors,
        typography = MoruTheme.typography,
        materialStyle = MoruMaterialStyle.Standard,
    ) {
        CompositionLocalProvider(LocalMoruNativeMaterial provides false, content = content)
    }
}
