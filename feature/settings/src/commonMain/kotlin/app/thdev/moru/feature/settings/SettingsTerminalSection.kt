package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruSettingSection
import app.thdev.moru.core.designsystem.component.MoruSettingSwitchRow
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.effectiveTerminalFontFamily
import app.thdev.moru.core.model.effectiveTerminalFontSize
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
internal fun SettingsTerminalSection(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
    scrollbackText: String,
    onScrollbackTextChange: (String) -> Unit,
    fontSizeText: String,
    onFontSizeTextChange: (String) -> Unit,
    fontFamilies: List<String>,
) {
    val strings = settingsStrings()
    MoruSettingSection(
        title = strings.terminalTitle,
        description = strings.terminalDescription,
        icon = MoruIconName.Terminal,
    ) {
        MoruSettingRow(title = strings.fontTitle, description = strings.fontDescription) {
            SettingsFontControls(
                family = settings.effectiveTerminalFontFamily, families = fontFamilies,
                size = settings.effectiveTerminalFontSize, sizeText = fontSizeText,
                label = strings.fontTitle, sizeLabel = strings.fontSizeTitle,
                tag = "settings-font", defaultFamily = FontFamily.Monospace,
                onFamilyChange = { font -> onChange { it.withTerminalFont(font, it.effectiveTerminalFontSize) } },
                onSizeChange = onFontSizeTextChange,
                onReset = {
                    onChange { it.withTerminalFont(null, 13) }
                    onFontSizeTextChange("13")
                },
            )
        }
        MoruSettingSwitchRow(
            title = strings.optionKeyTitle,
            description = strings.optionKeyDescription,
            checked = settings.terminal.optionSendsEscape,
            onCheckedChange = { enabled -> onChange { it.copy(terminal = it.terminal.copy(optionSendsEscape = enabled)) } },
            modifier = Modifier.testTag("settings-terminal-option-escape"),
        )
        MoruSettingRow(title = strings.shellTitle, description = strings.shellDescription) {
            SettingsTextInput(
                value = settings.terminal.shellCommand,
                label = strings.shellTitle,
                placeholder = "/bin/zsh",
                tag = "settings-shell",
                onChange = { shell -> onChange { it.copy(terminal = it.terminal.copy(shellCommand = shell)) } },
                modifier = Modifier.width(300.dp),
            )
        }
        MoruSettingRow(title = strings.scrollbackTitle, description = strings.scrollbackDescription, showDivider = false) {
            SettingsNumberInput(
                text = scrollbackText,
                range = 500..50000,
                label = strings.scrollbackTitle,
                tag = "settings-scrollback",
                onChange = onScrollbackTextChange,
                modifier = Modifier.width(300.dp),
            )
        }
    }
}
