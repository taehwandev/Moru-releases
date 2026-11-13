package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A small menu of text actions, opened from the control that owns it, on the same glass sheet as [MoruPopover].
 * Choosing one runs it and closes the menu. A long list scrolls inside a sheet of bounded height, so the menu stays
 * a compact window beside its control instead of one as tall as the screen. The rows sit inset inside the sheet, so a
 * highlighted row never touches its edge or rounded corners. With a [searchPlaceholder] a field on top narrows the
 * entries by label, for lists such as branches that grow long.
 */
@Composable
fun MoruDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<MoruMenuItem>,
    modifier: Modifier = Modifier,
    searchPlaceholder: String? = null,
) {
    var query by remember(expanded) { mutableStateOf("") }
    val shown = if (searchPlaceholder == null || query.isBlank()) items
        else items.filter { item -> item.label.contains(query.trim(), ignoreCase = true) }
    MoruPopover(
        expanded = expanded,
        onDismiss = onDismiss,
        modifier = modifier.widthIn(min = 180.dp, max = 360.dp).width(IntrinsicSize.Max).heightIn(max = 360.dp),
    ) {
        Column(Modifier.padding(6.dp)) {
            if (searchPlaceholder != null) {
                MoruTextField(query, { query = it }, Modifier.fillMaxWidth().padding(bottom = 4.dp), placeholder = searchPlaceholder)
            }
            shown.forEach { item ->
                MoruMenuRow(
                    label = item.label,
                    checked = item.checked,
                    icon = item.icon,
                    onClick = {
                        onDismiss()
                        item.onClick()
                    },
                )
            }
        }
    }
}
