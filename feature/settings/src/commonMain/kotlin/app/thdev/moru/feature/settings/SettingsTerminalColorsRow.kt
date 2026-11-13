package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruSettingOptionSelector
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.terminal.TerminalColorScheme
import app.thdev.moru.core.model.terminal.TerminalColorSlot
import app.thdev.moru.core.ui.terminal.moruTerminalColorScheme
import app.thdev.moru.feature.settings.i18n.settingsStrings

/**
 * Edits single colors of the light or dark scheme, with a preview. A color the user never set follows Moru, so it is
 * shown in Moru's color and clearing a field returns that slot to Moru.
 */
@Composable
internal fun SettingsTerminalColorsRow(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
) {
    val strings = settingsStrings()
    val current = MoruTheme.colors
    var light by remember { mutableStateOf(current.isLight) }
    var selected by remember { mutableStateOf(TerminalColorSlot.Background) }
    // Moru's colors for the other theme are the built-in ones; the current theme's are the ones on screen.
    val moru = moruTerminalColorScheme(if (light == current.isLight) current else if (light) lightCkColors else darkCkColors)
    val overrides = settings.terminal.profile?.colorsFor(light)
    val shown = overrides?.over(moru) ?: moru
    MoruSettingRow(title = strings.terminalColorsTitle, description = strings.terminalColorsDescription) {
        Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MoruSettingOptionSelector(
                options = listOf(LightMode, DarkMode),
                selectedOption = if (light) LightMode else DarkMode,
                onOptionSelected = { light = it == LightMode },
                optionLabel = { if (it == LightMode) strings.terminalSchemeLight else strings.terminalSchemeDark },
            )
            SettingsTerminalPreview(shown)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(TerminalColorSlot.Foreground, TerminalColorSlot.Background, TerminalColorSlot.Selection).forEach { slot ->
                    SettingsTerminalColorSwatch(slot, shown[slot], overrides?.get(slot) != null, slot == selected) { selected = slot }
                }
            }
            TerminalColorSlot.ansiSlots.chunked(8).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { slot ->
                        SettingsTerminalColorSwatch(slot, shown[slot], overrides?.get(slot) != null, slot == selected) { selected = slot }
                    }
                }
            }
            SettingsTerminalHexField(
                slot = selected,
                value = overrides?.get(selected),
                moruValue = moru[selected],
                onChange = { hex ->
                    onChange {
                        it.withTerminalProfile { profile ->
                            val scheme = profile.colorsFor(light) ?: TerminalColorScheme()
                            profile.withColors(light, scheme.with(selected, hex))
                        }
                    }
                },
            )
        }
    }
}

private const val LightMode = "light"
private const val DarkMode = "dark"
