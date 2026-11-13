package app.thdev.moru.feature.git.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuItem
import app.thdev.moru.core.designsystem.component.MoruSelectField

/**
 * A one-line ref picker; [label] names it for assistive tech while the field shows only the chosen ref. [iconOf] marks
 * each option's kind and [searchPlaceholder] adds a filter on top of a long list.
 */
@Composable
internal fun GitTargetSelector(
    label: String, selected: String, options: List<String>, enabled: Boolean,
    tag: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier,
    iconOf: (String) -> MoruIconName? = { null },
    searchPlaceholder: String? = null,
) {
    var expanded by remember(options, enabled) { mutableStateOf(false) }
    Box(modifier) {
        MoruSelectField(
            text = selected.ifBlank { "None" },
            onClick = { expanded = true },
            modifier = Modifier.testTag(tag).semantics { contentDescription = "$label: ${selected.ifBlank { "None" }}" },
            enabled = enabled && options.isNotEmpty(),
        )
        MoruDropdownMenu(
            expanded = expanded && enabled, onDismiss = { expanded = false },
            items = options.map { value -> MoruMenuItem(value, checked = value == selected, icon = iconOf(value)) { onSelect(value) } },
            modifier = Modifier.testTag("$tag-options"),
            searchPlaceholder = searchPlaceholder,
        )
    }
}
