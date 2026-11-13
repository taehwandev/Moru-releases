package app.thdev.moru.feature.codepanel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionManager
import kotlinx.coroutines.delay

private const val SelectionPollMillis = 250L

/**
 * The Code panel of the Task's chosen terminal. While it is shown, [TaskCodePanelStateHolder] reads, every two seconds
 * and off the UI thread, where the terminal's programs work and whether that checkout's changes moved, and rereads the
 * changes only when they did. Each terminal keeps its own checkout in [state].
 */
@Composable
fun TaskCodePanel(
    source: CodePanelSource,
    state: CodePanelState,
    taskKey: String,
    projectFolder: String,
    sessions: TerminalSessionManager,
    modifier: Modifier = Modifier,
) {
    // The chosen terminal changes without this screen recomposing, so its revision is read the way the terminal pane does.
    val revision by produceState(sessions.revision(), sessions) {
        while (true) {
            value = sessions.revision()
            delay(SelectionPollMillis)
        }
    }
    val terminal = remember(revision, taskKey) {
        val own = sessions.listSessions().filter { session ->
            session.spec.attachedItemId == taskKey && session.spec.kind == TerminalSessionKind.TERMINAL
        }
        own.firstOrNull { session -> session.id == sessions.selectedSessionId() } ?: own.firstOrNull()
    }
    val sessionId = terminal?.id
    val terminalKey = terminal?.let { session -> session.spec.hostKey ?: session.id } ?: taskKey

    // One holder per folder, so a different folder starts from no checkouts, as before.
    val holder = remember(source, state, projectFolder) { TaskCodePanelStateHolder(source, state) }
    // Both reads run git and lsof, so they keep going only while Moru's window has focus; on return they read at once.
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(holder, taskKey, terminalKey, sessionId, focused) {
        holder.followWorktrees(taskKey, terminalKey, sessionId, projectFolder, repeat = focused)
    }

    val root = holder.shown(taskKey, terminalKey)
    LaunchedEffect(holder, root, focused) { holder.watchChanges(root, repeat = focused) }

    CodePanel(
        fileCode = { worktreeRoot, file, fileModifier -> ChangedFileCode(source, worktreeRoot, file, fileModifier) },
        worktrees = holder.worktrees,
        checked = holder.checked,
        worktreeRoot = root,
        changes = holder.changes,
        onPickWorktree = { picked -> holder.pick(taskKey, terminalKey, picked) },
        modifier = modifier,
    )
}
