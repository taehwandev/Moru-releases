package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle

/** The colors a button of one [MoruButtonType] takes at rest, under the pointer and while pressed. */
internal data class MoruButtonStyle(val background: Color, val content: Color, val border: Color)

@Composable
internal fun moruButtonStyle(type: MoruButtonType, hovered: Boolean, pressed: Boolean): MoruButtonStyle {
    val colors = MoruTheme.colors
    val apple = MoruTheme.materialStyle == MoruMaterialStyle.Apple
    // The same faint wash of the text color darkens a light theme and lightens a dark one.
    val wash = when {
        pressed -> colors.textPrimary.copy(alpha = 0.14f)
        hovered -> colors.textPrimary.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    return when (type) {
        MoruButtonType.PRIMARY -> MoruButtonStyle(
            background = when {
                pressed -> Color.Black.copy(alpha = 0.18f).compositeOver(colors.primary)
                hovered -> (if (colors.isLight) Color.Black.copy(alpha = 0.08f)
                    else Color.White.copy(alpha = 0.10f)).compositeOver(colors.primary)
                else -> colors.primary
            },
            content = colors.onPrimary,
            border = Color.Transparent,
        )
        MoruButtonType.SECONDARY -> MoruButtonStyle(
            wash.compositeOver(if (apple) colors.surface.copy(alpha = 0.72f) else colors.surface),
            colors.textPrimary,
            if (!apple) colors.border else if (colors.isLight) colors.border.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.22f),
        )
        MoruButtonType.GHOST -> MoruButtonStyle(wash, colors.textPrimary, Color.Transparent)
        MoruButtonType.DANGER -> MoruButtonStyle(
            background = colors.error.copy(alpha = if (pressed) 0.22f else if (hovered) 0.14f else 0f),
            content = colors.error,
            border = colors.error.copy(alpha = 0.5f),
        )
    }
}
