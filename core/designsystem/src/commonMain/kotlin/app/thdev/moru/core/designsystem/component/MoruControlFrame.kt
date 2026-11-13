package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** The height every one-line control shares with a [MoruButtonSize.Medium] button, so a row of them lines up. */
val MoruControlHeight = MoruButtonSize.Medium.height

/** The corner a button and a one-line control share: a capsule in the Apple style, a small radius otherwise. */
@Composable
internal fun moruControlShape(): Shape =
    if (MoruTheme.materialStyle == MoruMaterialStyle.Apple) RoundedCornerShape(percent = 50)
    else RoundedCornerShape(MoruTheme.radius.small)

/**
 * The outline a field and a select draw: a secondary button's surface at [MoruControlHeight], a primary border while
 * [focused] and an error border while [error].
 */
@Composable
internal fun Modifier.moruControlFrame(
    focused: Boolean = false,
    error: Boolean = false,
    hovered: Boolean = false,
    enabled: Boolean = true,
): Modifier {
    val style = moruButtonStyle(MoruButtonType.SECONDARY, hovered = enabled && hovered, pressed = false)
    val border = when {
        error -> MoruTheme.colors.error
        focused -> MoruTheme.colors.primary
        else -> style.border
    }
    val shape = moruControlShape()
    return this
        .height(MoruControlHeight)
        .alpha(if (enabled) 1f else 0.45f)
        .clip(shape)
        .background(style.background)
        .then(if (border != Color.Transparent) Modifier.border(MoruTheme.stroke.thin, border, shape) else Modifier)
}
