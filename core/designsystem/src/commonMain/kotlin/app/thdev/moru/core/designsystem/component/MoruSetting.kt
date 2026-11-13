package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle

@Composable
fun MoruSettingSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: MoruIconName? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MoruSettingSectionHeader(
            title = title,
            description = description,
            icon = icon,
        )
        val shape = RoundedCornerShape(MoruTheme.radius.medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (MoruTheme.materialStyle == MoruMaterialStyle.Apple) Modifier.ckSurface(MoruSurfaceRole.Card)
                    else Modifier.clip(shape).background(MoruTheme.colors.surface)
                        .border(MoruTheme.stroke.thin, MoruTheme.colors.border, shape))
        ) {
            content()
        }
    }
}

@Composable
fun MoruSettingSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: MoruIconName? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(MoruTheme.radius.small))
                    .background(MoruTheme.colors.primary.copy(alpha = 0.12f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                MoruIcon(name = icon, size = 16.dp, tint = MoruTheme.colors.primary)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MoruText(
                text = title,
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            )
            if (description != null) {
                MoruText(
                    text = description,
                    style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
fun MoruSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: MoruIconName? = null,
    showDivider: Boolean = true,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                MoruIcon(name = icon, size = 16.dp, tint = MoruTheme.colors.textSecondary)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MoruText(
                    text = title,
                    style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold)
                )
                if (description != null) {
                    MoruText(
                        text = description,
                        style = MoruTheme.typography.caption,
                        color = MoruTheme.colors.textSecondary
                    )
                }
            }
            if (trailingContent != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailingContent
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color = MoruTheme.colors.border
            )
        }
    }
}

@Composable
fun MoruSettingOptionSelector(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minOptionWidth: Dp = 72.dp,
    wrapInFlow: Boolean = false,
    optionLabel: (String) -> String = { it }
) {
    val itemsContent: @Composable () -> Unit = {
        options.forEach { option ->
            val selected = option == selectedOption
            val optionBackground = if (selected) {
                MoruTheme.colors.primary
            } else {
                MoruTheme.colors.background
            }
            val optionTextColor = if (selected) {
                MoruTheme.colors.onPrimary
            } else {
                MoruTheme.colors.textPrimary
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = minOptionWidth, minHeight = 30.dp)
                    .clip(RoundedCornerShape(MoruTheme.radius.xSmall))
                    .background(optionBackground)
                    .clickable(enabled = enabled && !selected) { onOptionSelected(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                MoruText(
                    text = optionLabel(option),
                    style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = optionTextColor,
                    maxLines = 1
                )
            }
        }
    }

    if (wrapInFlow) {
        FlowRow(
            modifier = modifier
                .ckSurface(MoruSurfaceRole.Control)
                .padding(3.dp),
            // Wrapped options keep to the row's end edge, like every other trailing control.
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            itemsContent()
        }
    } else {
        Row(
            modifier = modifier
                .ckSurface(MoruSurfaceRole.Control)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsContent()
        }
    }
}

@Composable
fun MoruSettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: MoruIconName? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true
) {
    MoruSettingRow(
        title = title,
        description = description,
        icon = icon,
        modifier = modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
        showDivider = showDivider
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MoruTheme.colors.onPrimary,
                checkedTrackColor = MoruTheme.colors.primary,
                checkedBorderColor = MoruTheme.colors.primary,
                uncheckedThumbColor = MoruTheme.colors.textSecondary,
                uncheckedTrackColor = MoruTheme.colors.surface,
                uncheckedBorderColor = MoruTheme.colors.border
            )
        )
    }
}
