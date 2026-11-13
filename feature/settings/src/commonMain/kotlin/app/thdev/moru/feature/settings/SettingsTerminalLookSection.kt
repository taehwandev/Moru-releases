package app.thdev.moru.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruSettingSection
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.terminal.TerminalProfileImport
import app.thdev.moru.feature.settings.i18n.settingsStrings

/** Colors, cursor and line spacing of every terminal; anything left alone follows the Moru theme. */
@Composable
internal fun SettingsTerminalLookSection(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
    importTerminalProfile: suspend () -> TerminalProfileImport,
) {
    val strings = settingsStrings()
    MoruSettingSection(
        title = strings.terminalLookTitle,
        description = strings.terminalLookDescription,
        icon = MoruIconName.Palette,
    ) {
        SettingsTerminalSchemeRow(settings, onChange)
        SettingsTerminalColorsRow(settings, onChange)
        SettingsTerminalCursorRows(settings, onChange)
        SettingsTerminalLineSpacingRow(settings, onChange)
        SettingsTerminalImportRow(settings, onChange, importTerminalProfile)
        MoruSettingRow(title = strings.terminalLookResetTitle, description = strings.terminalLookResetDescription, showDivider = false) {
            MoruButton(
                text = strings.terminalLookResetAction,
                onClick = { onChange { it.withMoruTerminalLook() } },
                enabled = settings.terminal.profile != null,
                type = MoruButtonType.SECONDARY, size = MoruButtonSize.Small,
                modifier = Modifier.testTag("settings-terminal-look-reset"),
            )
        }
    }
}
