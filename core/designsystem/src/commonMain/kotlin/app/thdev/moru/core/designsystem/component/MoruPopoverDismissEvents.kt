package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.KeyEvent

/** Platform events cover input coming from embedded native controls outside the panel. */
@Composable
internal expect fun MoruPopoverDismissEvents(onDismiss: () -> Unit, onPreviewKeyEvent: (KeyEvent) -> Boolean)
