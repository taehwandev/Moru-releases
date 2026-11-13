package app.thdev.moru.feature.workbench.resume

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.workbench.i18n.workbenchStrings
import app.thdev.moru.feature.workbench.launch.AgentConversation
import app.thdev.moru.core.model.session.SessionActivity

/** Codex caches its terminal background at startup, so the user chooses when to restart. */
@Composable
internal fun ThemeRestartBar(
    session: TerminalSessionSnapshot,
    activity: SessionActivity,
    inProgress: Boolean,
    failed: Boolean,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = workbenchStrings()
    val enabled = themeRestartAllowed(session, activity, inProgress)
    Row(
        modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Card).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MoruText(strings.codexThemeChanged(session.title, session.id))
            MoruText(
                strings.codexThemeRestartDetails(AgentConversation.idIn(session.spec.startupCommand) != null),
                style = MoruTheme.typography.caption,
            )
            if (failed) MoruText(strings.themeRestartFailed, color = MoruTheme.colors.error, style = MoruTheme.typography.caption)
            if (inProgress || !enabled) MoruText(
                if (inProgress) strings.themeRestartInProgress else strings.themeRestartBusy,
                style = MoruTheme.typography.caption,
            )
        }
        MoruButton(strings.restartForTheme, onRestart, enabled = enabled)
    }
}
