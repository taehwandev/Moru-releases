package app.thdev.moru.feature.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import app.thdev.moru.core.terminal.TerminalSessionManager
import kotlinx.coroutines.delay

/** Metadata polling must not keep Compose's render clock active while a terminal is idle. */
@Composable
fun rememberTerminalRevision(manager: TerminalSessionManager): State<Long> {
    val initial = remember(manager) { manager.revision() }
    return produceState(initial, manager) {
        while (true) {
            value = manager.revision()
            delay(250)
        }
    }
}
