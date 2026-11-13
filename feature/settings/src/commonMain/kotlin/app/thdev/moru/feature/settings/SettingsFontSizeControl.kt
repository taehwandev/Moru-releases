package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruNumberStepper
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
internal fun SettingsFontSizeControl(text: String, label: String, tag: String, onChange: (String) -> Unit) {
    val range = 10..28
    val value = text.validSettingsNumber(range)
    val strings = settingsStrings()
    val errorMessage = strings.numberRange(range.first, range.last)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MoruNumberStepper(
            text = text,
            onTextChange = onChange,
            onDecrease = { value?.let { onChange((it - 1).toString()) } },
            onIncrease = { value?.let { onChange((it + 1).toString()) } },
            decreaseDescription = strings.decreaseFontSize(label),
            increaseDescription = strings.increaseFontSize(label),
            canDecrease = value != null && value > range.first,
            canIncrease = value != null && value < range.last,
            isError = value == null,
            fieldModifier = Modifier.testTag(tag).semantics {
                contentDescription = label
                if (value == null) error(errorMessage)
            },
            decreaseModifier = Modifier.testTag("$tag-decrease"),
            increaseModifier = Modifier.testTag("$tag-increase"),
        )
        if (value == null) MoruText(errorMessage, color = MoruTheme.colors.error, style = MoruTheme.typography.caption)
    }
}
