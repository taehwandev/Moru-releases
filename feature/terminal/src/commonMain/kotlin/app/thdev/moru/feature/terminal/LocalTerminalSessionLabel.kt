package app.thdev.moru.feature.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/**
 * Draws a session's name and state in its tab and pane header, given whether it is the selected one. The app supplies
 * the session list's own label so a tab reads like its sidebar row; null keeps the plain status dot and title.
 */
val LocalTerminalSessionLabel = staticCompositionLocalOf<(@Composable (TerminalSessionSnapshot, Boolean) -> Unit)?> { null }
