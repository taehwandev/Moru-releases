package app.thdev.moru.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.*
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun TaskLaunchStatus(
    title: String,
    failed: WorkbenchNavigation.Launch?,
    onRetry: (WorkbenchNavigation.Launch) -> Unit,
) {
    val strings = appStrings()
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MoruText(if (failed == null) strings.preparingTask else strings.taskCouldNotStart, style = MoruTheme.typography.h2)
            MoruText(title, color = MoruTheme.colors.textSecondary)
            if (failed != null) {
                MoruText(strings.noAgentLaunched)
                MoruButton(text = strings.retry, onClick = { onRetry(failed) })
            } else {
                MoruText(strings.switchWhilePreparing, style = MoruTheme.typography.caption)
            }
        }
    }
}
