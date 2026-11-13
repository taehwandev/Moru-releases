package app.thdev.moru.app

import androidx.compose.runtime.Composable

/** Platform window ownership is independent of About's content and short-lived pet state. */
@Composable
internal expect fun AboutMoruWindow(onDismiss: () -> Unit, content: @Composable () -> Unit)
