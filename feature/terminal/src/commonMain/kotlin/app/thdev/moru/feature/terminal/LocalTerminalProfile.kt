package app.thdev.moru.feature.terminal

import androidx.compose.runtime.compositionLocalOf
import app.thdev.moru.core.model.terminal.TerminalProfile

/** The profile imported from another terminal app that every terminal draws and types with; null keeps Moru's theme. */
val LocalTerminalProfile = compositionLocalOf<TerminalProfile?> { null }
