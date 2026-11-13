package app.thdev.moru.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** One entry of the Settings page list: the page's icon and name, filled while it is the page shown. */
@Composable
internal fun SettingsPageRow(title: String, icon: MoruIconName, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MoruTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val fill = when {
        selected -> colors.primary.copy(alpha = if (colors.isLight) 0.14f else 0.26f)
        hovered -> colors.primary.copy(alpha = if (colors.isLight) 0.07f else 0.12f)
        else -> Color.Transparent
    }
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 34.dp)
            .background(fill, RoundedCornerShape(MoruTheme.radius.medium))
            .hoverable(interaction)
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruIcon(name = icon, size = 15.dp, tint = if (selected) colors.primary else colors.textSecondary)
        MoruText(
            text = title,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            style = MoruTheme.typography.body.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
