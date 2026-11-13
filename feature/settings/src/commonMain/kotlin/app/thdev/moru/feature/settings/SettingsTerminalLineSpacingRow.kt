package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.feature.settings.i18n.settingsStrings
import kotlin.math.roundToInt

/** Line height as a percent of the font's own; Moru's is 106. */
@Composable
internal fun SettingsTerminalLineSpacingRow(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
) {
    val strings = settingsStrings()
    val percent = ((settings.terminal.profile?.lineSpacing ?: MoruLineSpacing) * 100).roundToInt()
    var text by remember { mutableStateOf(percent.toString()) }
    LaunchedEffect(percent) { if (text.validSettingsNumber(LineSpacingRange) != percent) text = percent.toString() }
    MoruSettingRow(title = strings.terminalLineSpacingTitle, description = strings.terminalLineSpacingDescription) {
        SettingsNumberInput(
            text = text,
            range = LineSpacingRange,
            label = strings.terminalLineSpacingTitle,
            tag = "settings-terminal-line-spacing",
            onChange = { typed ->
                text = typed
                typed.validSettingsNumber(LineSpacingRange)?.let { value ->
                    val spacing = (value / 100f).takeUnless { value == (MoruLineSpacing * 100).roundToInt() }
                    onChange { it.withTerminalProfile { profile -> profile.copy(lineSpacing = spacing) } }
                }
            },
            modifier = Modifier.width(300.dp),
        )
    }
}

private const val MoruLineSpacing = 1.06f
private val LineSpacingRange = 80..200
