package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Moves one content tree between its docked slot and a native window. Closing that window docks it. */
@Composable
expect fun MoruDetachablePane(
    detached: Boolean,
    onRestore: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    placeholder: @Composable (Modifier) -> Unit,
    content: @Composable (Modifier) -> Unit,
)
