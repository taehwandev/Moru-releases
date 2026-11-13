package app.thdev.moru.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** The first-use path contains one concrete action and no unowned terminal. */
@Composable
internal fun WorkbenchWelcome(
    projectName: String? = null,
    projectPath: String? = null,
    onOpenProject: () -> Unit,
    onNewTask: () -> Unit,
) {
    val strings = appStrings()
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MoruText(projectName ?: strings.welcomeEmptyTitle, style = MoruTheme.typography.h1)
            MoruText(
                if (projectName == null) strings.welcomeEmptyBody else strings.welcomeProjectBody,
                color = MoruTheme.colors.textSecondary,
            )
            projectPath?.let { MoruText(it, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary) }
            MoruButton(
                text = if (projectName == null) strings.openProject else strings.newTask,
                onClick = if (projectName == null) onOpenProject else onNewTask,
            )
            if (projectName == null) {
                MoruText(
                    strings.terminalHint,
                    style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary,
                )
            }
        }
    }
}
