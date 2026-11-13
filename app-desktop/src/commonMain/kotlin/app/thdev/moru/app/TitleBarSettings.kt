package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Settings stays reachable beside the bell without opening notification history first. */
@Composable
internal fun TitleBarSettings(onClick: () -> Unit) {
    MoruIconButton(
        onClick = onClick,
        description = "Settings menu",
        modifier = Modifier.testTag("title-bar-settings"),
        size = 26.dp,
    ) {
        MoruIcon(MoruIconName.Settings, size = 17.dp, tint = MoruTheme.colors.textSecondary)
    }
}
