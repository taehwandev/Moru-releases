package app.thdev.moru.app

import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.terminal.releaseTerminalSession

/**
 * Ends one terminal for good: the local record, the view this run attached, and the program in the terminal host. A
 * terminal Moru never showed this run has no attached view, so its host program is ended by its key, or it would keep
 * running with no Task to reach it.
 */
internal fun endTerminalSession(
    session: TerminalSessionSnapshot,
    sessions: TerminalSessionManager,
    terminateHeld: (hostKey: String) -> Unit,
) {
    runCatching { sessions.closeSession(session.id) }
    runCatching { releaseTerminalSession(session.id) }
    session.spec.hostKey?.let { key -> runCatching { terminateHeld(key) } }
}
