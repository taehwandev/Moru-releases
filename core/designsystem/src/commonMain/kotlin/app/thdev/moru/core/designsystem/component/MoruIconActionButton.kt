package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A square button showing only [icon], styled and sized like [MoruButton] so it sits in the same row as text buttons.
 * [description] is what a screen reader says, since no label is shown. [tint], when given, colors only the icon, so a
 * control can stand out while keeping the button's own surface.
 */
@Composable
fun MoruIconButton(
    icon: MoruIconName,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: MoruButtonType = MoruButtonType.GHOST,
    enabled: Boolean = true,
    size: MoruButtonSize = MoruButtonSize.Medium,
    tint: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val style = moruButtonStyle(type, hovered = enabled && hovered, pressed = enabled && pressed)
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .size(size.height)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(style.background)
            .then(if (style.border != Color.Transparent) Modifier.border(1.dp, style.border, shape) else Modifier)
            .hoverable(interaction, enabled)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        MoruIcon(icon, size = size.iconSize, tint = tint ?: style.content)
    }
}
