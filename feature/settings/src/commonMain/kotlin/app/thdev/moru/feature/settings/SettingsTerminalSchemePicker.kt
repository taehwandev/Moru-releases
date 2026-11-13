package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruMenuRow
import app.thdev.moru.core.designsystem.component.MoruPopover
import app.thdev.moru.core.designsystem.component.MoruSelectField
import app.thdev.moru.core.model.terminal.TerminalColorPresets
import app.thdev.moru.core.model.terminal.TerminalColorScheme
import app.thdev.moru.feature.settings.i18n.settingsStrings

/** A menu of Moru's own colors and the built-in schemes; a scheme the user edited shows as custom. */
@Composable
internal fun SettingsTerminalSchemePicker(
    scheme: TerminalColorScheme?,
    tag: String,
    onPick: (TerminalColorScheme?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = settingsStrings()
    var expanded by remember { mutableStateOf(false) }
    val label = when {
        scheme == null -> strings.terminalSchemeMoru
        else -> TerminalColorPresets.matching(scheme)?.name ?: strings.terminalSchemeCustom
    }
    Box(modifier) {
        MoruSelectField(text = label, onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().testTag(tag))
        MoruPopover(
            expanded = expanded,
            onDismiss = { expanded = false },
            modifier = Modifier.width(220.dp).padding(horizontal = 6.dp).testTag("$tag-menu"),
        ) {
            MoruMenuRow(
                label = strings.terminalSchemeMoru,
                checked = scheme == null,
                modifier = Modifier.testTag("$tag-moru"),
                onClick = { expanded = false; onPick(null) },
            )
            TerminalColorPresets.all.forEach { preset ->
                MoruMenuRow(
                    label = preset.name,
                    checked = preset.scheme == scheme,
                    modifier = Modifier.testTag("$tag-option-${preset.name}"),
                    onClick = { expanded = false; onPick(preset.scheme) },
                )
            }
        }
    }
}
