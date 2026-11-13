package app.thdev.moru.feature.git

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** A pencil, plus or minus in its own color, so a kind of change is told apart by shape as well as by color. */
@Composable
internal fun GitWorkingKindIcon(kind: GitWorkingKind, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    MoruIcon(kind.icon(), modifier, size = size, tint = kind.color())
}

private fun GitWorkingKind.icon(): MoruIconName = when (this) {
    GitWorkingKind.Modified -> MoruIconName.Pencil
    GitWorkingKind.Added -> MoruIconName.Plus
    GitWorkingKind.Deleted -> MoruIconName.Minus
}

@Composable
private fun GitWorkingKind.color(): Color = when (this) {
    GitWorkingKind.Modified -> MoruTheme.colors.warning
    GitWorkingKind.Added -> MoruTheme.colors.success
    GitWorkingKind.Deleted -> MoruTheme.colors.error
}
