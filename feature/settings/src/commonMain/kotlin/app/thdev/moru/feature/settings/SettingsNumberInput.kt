package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruCompactTextField
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
internal fun SettingsNumberInput(
    text: String,
    range: IntRange,
    label: String,
    tag: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val invalid = text.validSettingsNumber(range) == null
    val errorMessage = settingsStrings().numberRange(range.first, range.last)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MoruCompactTextField(
            value = text,
            onValueChange = onChange,
            isError = invalid,
            modifier = Modifier.fillMaxWidth().testTag(tag).semantics {
                contentDescription = label
                if (invalid) error(errorMessage)
            },
        )
        if (invalid) MoruText(errorMessage, color = MoruTheme.colors.error, style = MoruTheme.typography.caption)
    }
}
