package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A panel another feature puts into an arrangement it does not own, such as a device among a Task's terminals. [key]
 * names it in the saved arrangement and stays the same while it is shown; [title] names its tab, and [onClose], when
 * given, is the tab's close button. [content] draws it with the cell's modifier and the modifier for its header, which
 * makes the header the panel's drag handle.
 */
class MoruPanelCell(
    val key: String,
    val title: String,
    val onClose: (() -> Unit)? = null,
    val content: @Composable (modifier: Modifier, headerModifier: Modifier) -> Unit,
)
