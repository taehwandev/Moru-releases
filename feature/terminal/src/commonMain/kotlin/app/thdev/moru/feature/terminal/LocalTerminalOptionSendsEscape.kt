package app.thdev.moru.feature.terminal

import androidx.compose.runtime.compositionLocalOf

/** Whether Option (Alt) sends Escape before the key in every terminal; Moru's default is on. */
val LocalTerminalOptionSendsEscape = compositionLocalOf { true }
