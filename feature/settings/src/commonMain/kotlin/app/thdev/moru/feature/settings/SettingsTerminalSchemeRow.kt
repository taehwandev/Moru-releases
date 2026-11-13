package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.feature.settings.i18n.settingsStrings

/** Picks a color scheme for the light Moru theme and one for the dark. */
@Composable
internal fun SettingsTerminalSchemeRow(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
) {
    val strings = settingsStrings()
    MoruSettingRow(title = strings.terminalSchemeTitle, description = strings.terminalSchemeDescription) {
        Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(true to strings.terminalSchemeLight, false to strings.terminalSchemeDark).forEach { (light, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoruText(label, color = MoruTheme.colors.textSecondary, style = MoruTheme.typography.caption, modifier = Modifier.width(44.dp))
                    SettingsTerminalSchemePicker(
                        scheme = settings.terminal.profile?.colorsFor(light),
                        tag = if (light) "settings-terminal-scheme-light" else "settings-terminal-scheme-dark",
                        onPick = { scheme -> onChange { it.withTerminalProfile { profile -> profile.withColors(light, scheme) } } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
