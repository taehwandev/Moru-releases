package app.thdev.moru.feature.workbench.result

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuItem
import app.thdev.moru.feature.workbench.i18n.resultStrings

/**
 * Which Task's Result is shown, in the Result header: the selected Task and the Tasks beside it, as (id, title).
 * The button names the Task shown now; picking another reports its id.
 */
@Composable
internal fun ResultTaskPicker(
    shown: String,
    tasks: List<Pair<String, String>>,
    onPick: (taskId: String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val strings = resultStrings()
    val title = tasks.firstOrNull { it.first == shown }?.second ?: return
    Box {
        MoruButton(
            text = strings.resultOf(title),
            onClick = { open = true },
            type = MoruButtonType.GHOST,
            icon = MoruIconName.ChevronDown,
            size = MoruButtonSize.ExtraSmall,
        )
        MoruDropdownMenu(
            expanded = open,
            onDismiss = { open = false },
            items = tasks.map { (id, name) -> MoruMenuItem(name, checked = id == shown) { open = false; onPick(id) } },
        )
    }
}
