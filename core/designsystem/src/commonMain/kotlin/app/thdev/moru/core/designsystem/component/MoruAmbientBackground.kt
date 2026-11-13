package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.LocalMoruNativeMaterial

/** Low-contrast ambient light beneath the navigation and cards, resolved from the active theme. */
@Composable
fun Modifier.moruAmbientBackground(): Modifier {
    if (MoruTheme.materialStyle == MoruMaterialStyle.Standard) return background(MoruTheme.colors.background)
    if (LocalMoruNativeMaterial.current) return background(Color.Transparent)
    val colors = MoruTheme.colors
    return background(colors.background).drawWithCache {
        val radius = maxOf(size.width, size.height).coerceAtLeast(1f)
        val upperLight = Brush.radialGradient(
            listOf(colors.primary.copy(alpha = if (colors.isLight) 0.10f else 0.12f), Color.Transparent),
            center = Offset(size.width * 0.08f, 0f),
            radius = radius * 0.85f,
        )
        val lowerLight = Brush.radialGradient(
            listOf(colors.success.copy(alpha = 0.045f), Color.Transparent),
            center = Offset(size.width, size.height),
            radius = radius * 0.75f,
        )
        onDrawBehind {
            drawRect(upperLight)
            drawRect(lowerLight)
        }
    }
}
