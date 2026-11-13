package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruCompactTextField
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.terminal.TerminalColorSlot
import app.thdev.moru.feature.settings.i18n.settingsStrings

/** Types the selected color as `#rrggbb`; an empty field or "Use Moru" gives the slot back to Moru. */
@Composable
internal fun SettingsTerminalHexField(
    slot: TerminalColorSlot,
    value: String?,
    moruValue: String?,
    onChange: (String?) -> Unit,
) {
    val strings = settingsStrings()
    var text by remember(slot) { mutableStateOf(value.orEmpty()) }
    LaunchedEffect(slot, value) { if (text.hexOrNull() != value) text = value.orEmpty() }
    val invalid = text.isNotBlank() && text.hexOrNull() == null
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MoruText(
            strings.terminalColorSlot(slot), style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textSecondary, modifier = Modifier.width(76.dp),
        )
        MoruCompactTextField(
            value = text,
            onValueChange = { typed ->
                text = typed
                when {
                    typed.isBlank() -> onChange(null)
                    else -> typed.hexOrNull()?.let(onChange)
                }
            },
            placeholder = moruValue,
            isError = invalid,
            modifier = Modifier.width(110.dp).testTag("settings-terminal-color-hex")
                .semantics { contentDescription = strings.terminalColorSlot(slot) },
        )
        MoruButton(
            text = strings.terminalColorUseMoru,
            onClick = { text = ""; onChange(null) },
            enabled = value != null,
            type = MoruButtonType.GHOST, size = MoruButtonSize.Small,
            modifier = Modifier.testTag("settings-terminal-color-use-moru"),
        )
    }
}

/** `#a1b2c3` or `a1b2c3` → `#a1b2c3`, else null. */
internal fun String.hexOrNull(): String? {
    val digits = trim().removePrefix("#")
    if (digits.length != 6 || !digits.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) return null
    return "#" + digits.lowercase()
}
