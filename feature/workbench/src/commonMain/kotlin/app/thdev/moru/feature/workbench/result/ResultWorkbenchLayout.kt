package app.thdev.moru.feature.workbench.result

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDetachablePane
import app.thdev.moru.core.designsystem.component.MoruResizableShareSplitPane

/**
 * The Task terminal beside its Result, at the width share [pane] keeps for the Task, with the Result's tool [strip]
 * on the right edge. The share survives folding and detaching while the terminal stays in its existing left slot; a
 * folded or detached Result leaves only the strip. The resize handle overlays the seam at full height instead of
 * reserving a gutter, and takes the pointer above both panes' native content.
 */
@Composable
internal fun ResultWorkbenchLayout(
    taskKey: String,
    pane: ResultPaneState,
    resultOpen: Boolean,
    dividerLabel: String,
    resetLabel: String,
    modifier: Modifier = Modifier,
    detached: Boolean = false,
    detachedTitle: String = "",
    onRestoreResult: () -> Unit = {},
    terminal: @Composable (Modifier) -> Unit,
    result: @Composable (Modifier) -> Unit,
    strip: @Composable (Modifier) -> Unit,
) {
    val attachedOpen = resultOpen && !detached
    // A folded or detached Result is as narrow as the strip, so a zero share pins it there.
    MoruResizableShareSplitPane(
        rightShare = if (attachedOpen) pane.share(taskKey) else 0f,
        onRightShareChange = { pane.setShare(taskKey, it) },
        onReset = { pane.resetShare(taskKey) },
        dividerLabel = dividerLabel,
        resetLabel = resetLabel,
        minLeftWidth = 260.dp,
        minRightWidth = if (attachedOpen) 276.dp else 36.dp,
        modifier = modifier.fillMaxSize(),
        resizable = attachedOpen,
        dividerOverlayModifier = Modifier.fillMaxHeight(),
        left = terminal,
        right = { slotModifier ->
            Row(slotModifier) {
                if (resultOpen || detached) {
                    MoruDetachablePane(
                        detached = detached,
                        onRestore = onRestoreResult,
                        title = detachedTitle,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        placeholder = {},
                        content = result,
                    )
                }
                strip(Modifier)
            }
        },
    )
}
