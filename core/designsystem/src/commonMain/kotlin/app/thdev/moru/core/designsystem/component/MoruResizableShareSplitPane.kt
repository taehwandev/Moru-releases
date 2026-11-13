package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * [MoruResizableSplitPane] whose right width is a share (0..1) of the split's width, so the preference keeps its
 * proportion when the window is resized. Supplying [dividerOverlayModifier] centers the 8dp resize target on the
 * seam between the panes instead of reserving a lane; it takes the pointer even over native pane content.
 */
@Composable
fun MoruResizableShareSplitPane(
    rightShare: Float,
    onRightShareChange: (Float) -> Unit,
    onReset: () -> Unit,
    dividerLabel: String,
    resetLabel: String,
    minLeftWidth: Dp,
    minRightWidth: Dp,
    modifier: Modifier = Modifier,
    resizable: Boolean = true,
    dividerOverlayModifier: Modifier? = null,
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit,
) = ResizableSplitLayout(
    rightWidth = { available -> available * rightShare },
    onRightWidthChange = { width, available ->
        if (available.value > 0f) onRightShareChange((width / available).coerceIn(0f, 1f))
    },
    onReset, dividerLabel, resetLabel, minLeftWidth, minRightWidth, modifier, resizable, left, right,
    dividerOverlayModifier = dividerOverlayModifier,
)
