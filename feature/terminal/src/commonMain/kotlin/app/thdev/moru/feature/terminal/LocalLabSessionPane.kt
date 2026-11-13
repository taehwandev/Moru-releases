package app.thdev.moru.feature.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/**
 * Draws a Lab session's conversation in its pane. The app provides it, because the Lab screen is another feature;
 * without it a Lab session shows only its title.
 */
val LocalLabSessionPane = staticCompositionLocalOf<(@Composable (session: TerminalSessionSnapshot, focused: Boolean, modifier: Modifier) -> Unit)?> { null }
