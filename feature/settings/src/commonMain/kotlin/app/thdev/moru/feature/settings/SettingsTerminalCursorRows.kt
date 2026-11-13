package app.thdev.moru.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruSettingOptionSelector
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruSettingSwitchRow
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.terminal.TerminalCursorStyle
import app.thdev.moru.feature.settings.i18n.settingsStrings

/** The cursor a program gets until it asks for another, and whether it blinks; Moru's is a blinking block. */
@Composable
internal fun SettingsTerminalCursorRows(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
) {
    val strings = settingsStrings()
    val profile = settings.terminal.profile
    // A profile that picked a shape but said nothing about blinking keeps that shape steady, as an import does.
    val blinks = profile?.cursorBlink ?: (profile?.cursor == null)
    MoruSettingRow(title = strings.terminalCursorTitle, description = strings.terminalCursorDescription) {
        MoruSettingOptionSelector(
            options = TerminalCursorStyle.entries.map { it.name },
            selectedOption = (profile?.cursor ?: TerminalCursorStyle.Block).name,
            onOptionSelected = { name ->
                val style = TerminalCursorStyle.valueOf(name).takeUnless { it == TerminalCursorStyle.Block }
                // Blinking stays as it is shown; a new shape alone must not stop it.
                onChange { it.withTerminalProfile { current -> current.copy(cursor = style, cursorBlink = blinks.takeUnless { on -> on && style == null }) } }
            },
            optionLabel = { strings.terminalCursorStyle(TerminalCursorStyle.valueOf(it)) },
            modifier = Modifier.testTag("settings-terminal-cursor"),
        )
    }
    MoruSettingSwitchRow(
        title = strings.terminalCursorBlinkTitle,
        description = strings.terminalCursorBlinkDescription,
        checked = blinks,
        onCheckedChange = { on -> onChange { it.withTerminalProfile { current -> current.copy(cursorBlink = on) } } },
        modifier = Modifier.testTag("settings-terminal-cursor-blink"),
    )
}
