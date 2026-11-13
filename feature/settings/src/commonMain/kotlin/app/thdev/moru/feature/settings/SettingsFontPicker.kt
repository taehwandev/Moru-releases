package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruMenuRow
import app.thdev.moru.core.designsystem.component.MoruPopover
import app.thdev.moru.core.designsystem.component.MoruSelectField
import app.thdev.moru.core.designsystem.component.MoruTextField
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
internal fun SettingsFontPicker(
    family: String?,
    families: List<String>,
    label: String,
    tag: String,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = settingsStrings()
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    Box(modifier) {
        MoruSelectField(
            text = family ?: strings.fontPlaceholder,
            onClick = { query = ""; expanded = true },
            modifier = Modifier.fillMaxWidth().testTag(tag).semantics { contentDescription = label },
        )
        MoruPopover(
            expanded = expanded,
            onDismiss = { expanded = false },
            modifier = Modifier.width(280.dp).heightIn(max = 360.dp).padding(horizontal = 6.dp).testTag("$tag-menu"),
        ) {
            MoruTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = strings.searchFonts,
                modifier = Modifier.testTag("$tag-search").semantics { contentDescription = strings.searchFonts },
            )
            MoruMenuRow(
                label = strings.fontPlaceholder,
                checked = family == null,
                modifier = Modifier.testTag("$tag-default"),
                onClick = { expanded = false; onChange(null) },
            )
            val matches = families.filter { it.contains(query, ignoreCase = true) }
            matches.forEach { name ->
                MoruMenuRow(
                    label = name,
                    checked = name == family,
                    modifier = Modifier.testTag("$tag-option-$name"),
                    onClick = { expanded = false; onChange(name) },
                )
            }
            if (matches.isEmpty()) MoruMenuRow(label = strings.noFontsFound, onClick = {}, enabled = false)
        }
    }
}
