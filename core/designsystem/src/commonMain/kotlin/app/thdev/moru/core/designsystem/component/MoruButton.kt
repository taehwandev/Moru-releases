package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * How much a button asks for attention: [PRIMARY] is the one main action of a surface, [SECONDARY] an ordinary
 * action with an outline, [GHOST] a quiet action that shows its shape only under the pointer, and [DANGER] an action
 * that removes something.
 */
enum class MoruButtonType {
    PRIMARY, SECONDARY, GHOST, DANGER
}

/** [Medium] for a panel's actions, [Small] for actions repeated on rows and in section headings, [ExtraSmall] for compact inline or tight row controls. */
enum class MoruButtonSize(internal val height: Dp, internal val horizontalPadding: Dp, internal val iconSize: Dp) {
    Medium(height = 30.dp, horizontalPadding = 12.dp, iconSize = 14.dp),
    Small(height = 24.dp, horizontalPadding = 8.dp, iconSize = 13.dp),
    ExtraSmall(height = 20.dp, horizontalPadding = 6.dp, iconSize = 11.dp),
}

/**
 * The app's one text button: the same height, corner and padding everywhere, an optional leading [icon], and a
 * hover and press state. A disabled button keeps its shape and fades, so a row of buttons does not change look
 * when some of them cannot be used.
 */
@Composable
fun MoruButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: MoruButtonType = MoruButtonType.PRIMARY,
    enabled: Boolean = true,
    icon: MoruIconName? = null,
    size: MoruButtonSize = MoruButtonSize.Medium,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val style = moruButtonStyle(type, hovered = enabled && hovered, pressed = enabled && pressed)
    val shape = moruControlShape()
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = size.height)
            .height(size.height)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(style.background)
            .then(if (style.border != Color.Transparent) Modifier.border(1.dp, style.border, shape) else Modifier)
            .hoverable(interaction, enabled)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = size.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) MoruIcon(icon, size = size.iconSize, tint = style.content)
        MoruText(
            text = text,
            style = MoruTheme.typography.body.copy(
                fontSize = when (size) {
                    MoruButtonSize.ExtraSmall -> 11.sp
                    MoruButtonSize.Small -> 12.sp
                    MoruButtonSize.Medium -> 13.sp
                },
                fontWeight = if (type == MoruButtonType.PRIMARY) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = style.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
