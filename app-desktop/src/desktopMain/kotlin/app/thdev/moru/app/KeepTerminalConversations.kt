package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import app.thdev.moru.core.data.defaultDesktopTerminalRecoveryStore
import app.thdev.moru.core.terminal.SessionRunningAgents
import app.thdev.moru.core.terminal.TerminalSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One application-wide checkpoint loop, including hidden Tasks and unfocused windows. */
@Composable
internal fun rememberTerminalConversationCheckpoint(sessions: TerminalSessionManager, layout: WorkbenchLayoutKeeper): suspend () -> Unit {
    val recovery = remember { defaultDesktopTerminalRecoveryStore(resolveMoruAppDataDir()) }
    val lock = remember { Mutex() }
    val checkpoint: suspend () -> Unit = remember(sessions, layout, recovery) {
        { lock.withLock {
            withContext(Dispatchers.IO) { SessionRunningAgents.refresh() }
            checkpointTerminalConversations(sessions, layout, { id ->
                if (SessionRunningAgents.agentFor(id) == null) null else readDesktopAgentConversation(id)
            }, recovery::remember)
        } }
    }
    LaunchedEffect(checkpoint) {
        while (true) {
            checkpoint()
            delay(ConversationCheckpointMillis)
        }
    }
    return checkpoint
}

private const val ConversationCheckpointMillis = 5_000L
