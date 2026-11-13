package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * One line of a glass [MoruPopover], drawn like a macOS menu item: a soft rounded highlight follows the pointer and a
 * [checked] line carries a check mark in its own column, so the labels stay aligned. A long label wraps to a second
 * line instead of being cut, since branch and font names are told apart by their ends. A [detail] line under the label
 * says what the item is, such as what a model is for.
 */
@Composable
fun MoruMenuRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    icon: MoruIconName? = null,
    detail: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val colors = MoruTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 30.dp)
            .background(
                if (hovered && enabled) colors.primary.copy(alpha = if (colors.isLight) 0.12f else 0.28f) else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // An unchecked row's icon takes the check column, so its label lines up with the plain rows'.
        Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            if (checked) MoruIcon(name = MoruIconName.Check, size = 12.dp, tint = colors.textSecondary)
            else if (icon != null) MoruIcon(name = icon, size = 14.dp, tint = colors.textSecondary)
        }
        if (checked && icon != null) MoruIcon(name = icon, size = 14.dp, tint = colors.textSecondary)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            MoruText(
                text = label,
                color = if (enabled) colors.textPrimary else colors.textSecondary,
                style = MoruTheme.typography.body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) MoruText(detail, color = colors.textSecondary, style = MoruTheme.typography.caption, maxLines = 2)
        }
    }
}
