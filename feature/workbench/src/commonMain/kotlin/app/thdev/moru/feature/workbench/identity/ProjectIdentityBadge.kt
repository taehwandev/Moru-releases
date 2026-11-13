package app.thdev.moru.feature.workbench.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.identity.projectIdentityAccent

/** The popover's Project identity rule, shared by every Project navigation surface. */
@Composable
internal fun ProjectIdentityBadge(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    active: Boolean = false,
) {
    val color = projectIdentityAccent(name).color(MoruTheme.colors)
    val cornerRadius = if (size < 24.dp) MoruTheme.radius.xSmall else MoruTheme.radius.small
    val shape = RoundedCornerShape(cornerRadius)
    val fontSize = if (size < 24.dp) MoruTheme.typography.micro.fontSize else MoruTheme.typography.caption.fontSize
    val fillAlpha = if (active) 0.24f else 0.14f
    val badgeModifier = modifier
        .size(size)
        .background(color.copy(alpha = fillAlpha), shape)
    Box(badgeModifier, contentAlignment = Alignment.Center) {
        MoruText(
            name.take(2).uppercase(),
            style = MoruTheme.typography.caption.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = fontSize,
            ),
            color = color,
            maxLines = 1,
        )
    }
}
