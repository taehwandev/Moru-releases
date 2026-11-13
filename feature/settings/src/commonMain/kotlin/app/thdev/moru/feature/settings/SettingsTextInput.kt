package app.thdev.moru.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.thdev.moru.core.designsystem.component.MoruCompactTextField

@Composable
internal fun SettingsTextInput(
    value: String?,
    label: String,
    placeholder: String,
    tag: String,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value.orEmpty()) }
    // Only a value that differs from what the typed text means replaces it, so trimming never rewrites the field.
    LaunchedEffect(value) { if (text.asSettingValue() != value) text = value.orEmpty() }
    MoruCompactTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it.asSettingValue())
        },
        placeholder = placeholder,
        modifier = modifier.testTag(tag).semantics { contentDescription = label },
    )
}

private fun String.asSettingValue(): String? = trim().takeIf(String::isNotEmpty)
