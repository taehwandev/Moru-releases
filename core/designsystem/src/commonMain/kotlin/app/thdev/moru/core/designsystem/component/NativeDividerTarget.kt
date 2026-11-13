package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Mouse target for a divider overlaid on native pane content, which would otherwise take the pointer before any
 * Compose handle under it. Draws nothing; scenes without a native window leave the Compose handle in charge. A
 * [vertical] target reports up and down drags, for a seam between stacked panes.
 */
@Composable
internal expect fun NativeDividerTarget(
    onDragDelta: (Dp) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier,
    vertical: Boolean = false,
)
