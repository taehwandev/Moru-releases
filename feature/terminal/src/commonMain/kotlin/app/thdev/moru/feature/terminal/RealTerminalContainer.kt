package app.thdev.moru.feature.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The visible terminal. It acquires the retained PTY for [sessionKey], which the desktop
 * launch path may have started before this view was mounted, and reports what it sees:
 * [onOutput] with the wall-clock time of each output chunk, and [onExit] with the exit
 * code (null if unknown) when the process ends.
 */
@Composable
expect fun RealTerminalContainer(
    modifier: Modifier = Modifier,
    sessionKey: String,
    workingDirectory: String,
    shell: String,
    startupCommand: String?,
    onCommandCaptured: (String) -> Unit,
    fontFamily: String?,
    fontSize: Int,
    scrollbackLines: Int,
    onOutput: (atMillis: Long) -> Unit = {},
    onExit: (exitCode: Int?) -> Unit = {},
    /** Whether this is the chosen terminal; only it can receive explicit keyboard focus requests. */
    focused: Boolean = false,
    /** The terminal's name in the terminal host, so its program outlives Moru; null runs it inside Moru. */
    hostKey: String? = null,
    /** Native mouse/keyboard focus must select the same session as the Compose tabs. */
    onFocus: () -> Unit = {},
)
