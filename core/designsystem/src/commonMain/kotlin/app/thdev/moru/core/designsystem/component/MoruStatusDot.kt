package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
fun MoruStatusDot(
    type: MoruBadgeType,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
) {
    val color = when (type) {
        MoruBadgeType.SUCCESS -> MoruTheme.colors.success
        MoruBadgeType.WARNING -> MoruTheme.colors.warning
        MoruBadgeType.ERROR -> MoruTheme.colors.error
        MoruBadgeType.INFO -> MoruTheme.colors.primary
        MoruBadgeType.NEUTRAL -> MoruTheme.colors.textSecondary
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(MoruTheme.radius.xSmall))
            .background(color),
    )
}
