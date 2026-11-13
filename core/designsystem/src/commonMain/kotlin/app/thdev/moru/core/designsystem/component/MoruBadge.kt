package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp

enum class MoruBadgeType {
    SUCCESS, WARNING, ERROR, NEUTRAL, INFO
}

@Composable
fun MoruBadge(
    text: String,
    modifier: Modifier = Modifier,
    type: MoruBadgeType = MoruBadgeType.NEUTRAL,
    icon: MoruIconName? = null,
) {
    val backgroundColor = when (type) {
        MoruBadgeType.SUCCESS -> MoruTheme.colors.success.copy(alpha = 0.15f)
        MoruBadgeType.WARNING -> MoruTheme.colors.warning.copy(alpha = 0.15f)
        MoruBadgeType.ERROR -> MoruTheme.colors.error.copy(alpha = 0.15f)
        MoruBadgeType.INFO -> MoruTheme.colors.primary.copy(alpha = 0.15f)
        MoruBadgeType.NEUTRAL -> MoruTheme.colors.textSecondary.copy(alpha = 0.12f)
    }

    val contentColor = when (type) {
        MoruBadgeType.SUCCESS -> MoruTheme.colors.success
        MoruBadgeType.WARNING -> MoruTheme.colors.warning
        MoruBadgeType.ERROR -> MoruTheme.colors.error
        MoruBadgeType.INFO -> MoruTheme.colors.primary
        MoruBadgeType.NEUTRAL -> MoruTheme.colors.textSecondary
    }

    val shape = RoundedCornerShape(MoruTheme.radius.xSmall)

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(MoruTheme.stroke.thin, contentColor.copy(alpha = 0.25f), shape)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { MoruIcon(name = it, size = 10.dp, tint = contentColor) }
            MoruText(
                text = text.uppercase(),
                color = contentColor,
                style = MoruTheme.typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

