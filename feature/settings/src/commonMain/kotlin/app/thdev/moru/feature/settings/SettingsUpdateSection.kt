package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruSettingSection
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
internal fun SettingsUpdateSection(state: SettingsUpdateState, onCheck: () -> Unit, onRestart: () -> Unit) {
    val strings = settingsStrings()
    MoruSettingSection(
        title = strings.appUpdatesTitle,
        description = strings.updatesDescription,
        icon = MoruIconName.Download,
        modifier = Modifier.testTag("settings-updates"),
    ) {
        MoruSettingRow(
            title = state.currentVersion?.let(strings::currentVersion) ?: strings.versionUnavailable,
            description = if (state.action == SettingsUpdateAction.Unavailable) strings.updatesUnavailable
                else strings.automaticUpdates,
            showDivider = state.message != null,
        ) {
            val restart = state.action == SettingsUpdateAction.Restart
            MoruButton(
                text = if (restart) strings.restartToUpdate else strings.checkForUpdates,
                onClick = if (restart) onRestart else onCheck,
                enabled = state.action == SettingsUpdateAction.Check || restart,
                modifier = Modifier.testTag("settings-update-action"),
            )
        }
        state.message?.let { message ->
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                MoruText(
                    text = message,
                    color = if (state.failed) MoruTheme.colors.warning else MoruTheme.colors.textSecondary,
                    style = MoruTheme.typography.caption,
                    modifier = Modifier.testTag("settings-update-status"),
                )
            }
        }
    }
}
