package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Two stable slots separated by an accessible divider. The caller owns the width
 * preference and reset behavior; this primitive fits it to the current window.
 */
@Composable
fun MoruResizableSplitPane(
    rightWidth: Dp,
    onRightWidthChange: (Dp) -> Unit,
    onReset: () -> Unit,
    dividerLabel: String,
    resetLabel: String,
    minLeftWidth: Dp,
    minRightWidth: Dp,
    modifier: Modifier = Modifier,
    resizable: Boolean = true,
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit,
) = ResizableSplitLayout(
    rightWidth = { rightWidth },
    onRightWidthChange = { width, _ -> onRightWidthChange(width) },
    onReset, dividerLabel, resetLabel, minLeftWidth, minRightWidth, modifier, resizable, left, right,
)
