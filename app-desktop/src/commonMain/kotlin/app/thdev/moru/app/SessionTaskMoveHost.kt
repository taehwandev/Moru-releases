package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.canAddTaskTerminalSession
import app.thdev.moru.feature.workbench.restore.savedTerminalsOf

/** Owns the chooser lifetime and saves successful moves through the existing terminal layout. */
@Composable
internal fun WithSessionTaskMoves(
    navigation: WorkbenchNavigation,
    sessions: TerminalSessionManager,
    revision: Long,
    layout: WorkbenchLayoutKeeper?,
    onMoved: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    var requested by remember(navigation, sessions) { mutableStateOf<Pair<String, String?>?>(null) }
    // Read the revision so a destination that fills while the chooser is open becomes disabled.
    val snapshots = remember(sessions, revision, requested) { sessions.listSessions() }
    CompositionLocalProvider(LocalSessionTaskMove provides { sessionId ->
        navigation.reload()
        requested = sessionId to sessions.listSessions().firstOrNull { it.id == sessionId }?.spec?.attachedItemId
    }) { content() }
    requested?.let { (sessionId, sourceTaskId) ->
        val session = snapshots.firstOrNull { it.id == sessionId }
        val targets = SessionTaskMove.targets(navigation.sidebar, session)
        val full = targets.filterNot { sessions.canAddTaskTerminalSession(it.id) }.mapTo(mutableSetOf()) { it.id }
        SessionTaskMoveDialog(
            sessionId = sessionId,
            title = session?.title ?: "Session unavailable",
            workingDirectory = session?.spec?.workingDirectory.orEmpty(),
            targets = targets,
            fullTaskIds = full,
            onDismiss = { requested = null },
            onMove = { taskId ->
                SessionTaskMove.move(navigation, sessions, sessionId, sourceTaskId, taskId).onSuccess {
                    layout?.update { saved ->
                        val live = sessions.listSessions()
                        saved.copy(terminals = savedTerminalsOf(live), selectedTerminal = live.indexOfFirst { it.id == sessionId })
                    }
                    requested = null
                    onMoved(sessionId)
                }
            },
        )
    }
}
