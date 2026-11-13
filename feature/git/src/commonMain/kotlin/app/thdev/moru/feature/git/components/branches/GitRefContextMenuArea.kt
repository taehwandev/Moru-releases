package app.thdev.moru.feature.git.components.branches

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruMenuRow
import app.thdev.moru.core.designsystem.component.MoruPopover

/**
 * Wraps a ref row so a secondary click (right click, or Control-click on macOS) opens its commands in a menu beside
 * it. The row's own primary click is untouched. An empty [items] list opens nothing.
 */
@Composable
internal fun GitRefContextMenuArea(
    items: () -> List<GitRefMenuItem>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf<List<GitRefMenuItem>?>(null) }
    // The pointer loop outlives recompositions, so it reads the latest items instead of the first ones it saw.
    val currentItems by rememberUpdatedState(items)
    Box(modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    open = currentItems().takeIf { it.isNotEmpty() }
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }) {
        content()
        val menu = open
        MoruPopover(expanded = menu != null, onDismiss = { open = null },
            modifier = Modifier.widthIn(min = 180.dp, max = 320.dp).width(IntrinsicSize.Max).heightIn(max = 360.dp)) {
            Column(Modifier.padding(6.dp).testTag("git-ref-menu")) {
                menu.orEmpty().forEach { item ->
                    MoruMenuRow(
                        label = item.label,
                        onClick = { open = null; item.onClick() },
                        modifier = Modifier.testTag(item.tag),
                        enabled = item.enabled,
                    )
                }
            }
        }
    }
}
