package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.terminal.releaseTerminalSession

/**
 * Closing the terminal in view from the keyboard, after the user confirms. [request] only remembers which terminal
 * is meant; nothing stops until [confirm], or until [request] asks again for the same terminal, so the shortcut
 * pressed twice closes it. The terminal, a Lab included, is the Task's selected one, or its first when the selected
 * one belongs to another Task. [close] ends the confirmed terminal; by default at once, while the app first notes
 * which conversation its agent holds, so it can be opened again.
 */
internal class TerminalClose(
    private val sessions: TerminalSessionManager,
    private val close: (TerminalSessionSnapshot) -> Unit = { session -> closeTerminalNow(sessions, session.id) },
) {
    var pendingSessionId by mutableStateOf<String?>(null)
        private set

    /** Whether [taskId] has a terminal to close. */
    fun hasTerminal(taskId: String?): Boolean = taskId != null && sessionOf(taskId) != null

    fun request(taskId: String?) {
        val id = taskId?.let(::sessionOf)
        if (id != null && id == pendingSessionId) confirm() else pendingSessionId = id
    }

    fun keep() {
        pendingSessionId = null
    }

    fun confirm() {
        val id = pendingSessionId ?: return
        pendingSessionId = null
        sessions.listSessions().firstOrNull { it.id == id }?.let(close)
    }

    private fun sessionOf(taskId: String): String? {
        val own = sessions.listSessions().filter { session -> session.spec.attachedItemId == taskId }
        return own.firstOrNull { session -> session.id == sessions.selectedSessionId() }?.id ?: own.firstOrNull()?.id
    }
}

/** Ends the terminal [id] and forgets it. */
internal fun closeTerminalNow(sessions: TerminalSessionManager, id: String) {
    runCatching { sessions.closeSession(id) }
    runCatching { releaseTerminalSession(id) }
}
