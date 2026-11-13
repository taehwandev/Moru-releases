package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.thdev.moru.core.terminal.TerminalTeardown
import app.thdev.moru.feature.terminal.releaseAllTerminalSessions
import app.thdev.moru.feature.terminal.terminateAllTerminalSessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shared quit action: the UI remains responsive while terminal teardown finishes, then the app exits. A terminal that
 * fails to let go does not keep Moru open: the app still exits, and the failure goes to the log. [beforeExit] runs off
 * the UI thread just before the app exits.
 */
@Composable
internal fun rememberTerminalQuit(beforeTeardown: suspend () -> Unit = {}, beforeExit: () -> Unit = {}, exit: () -> Unit): (Boolean) -> Unit {
    val scope = rememberCoroutineScope()
    var quitting by remember { mutableStateOf(false) }
    return { keep ->
        if (!quitting) {
            quitting = true
            scope.launch {
                try {
                    runCatching { beforeTeardown() }
                        .onFailure { error -> System.err.println("Moru could not checkpoint terminals before quit: $error") }
                    withContext(Dispatchers.IO) {
                        runCatching { if (keep) releaseAllTerminalSessions() else terminateAllTerminalSessions() }
                            .onFailure { error -> System.err.println("Moru could not end every terminal on quit: $error") }
                        runCatching(beforeExit)
                        TerminalTeardown.finish()
                    }
                } finally {
                    exit()
                }
            }
        }
    }
}
