package app.thdev.moru.feature.workbench.restore

import app.thdev.moru.core.model.WorkbenchLayout
import app.thdev.moru.core.terminal.TerminalSessionManager

/**
 * Brings back the terminals of [layout] in [sessions] and chooses the one that was chosen. Only the terminals' records
 * come back here. When its Task is next shown, each one reattaches to its program in the terminal host if that is
 * still running, and only otherwise starts again, so a restart does not launch every agent at once.
 */
fun restoreTerminals(sessions: TerminalSessionManager, layout: WorkbenchLayout) {
    val restored = layout.terminals.map { saved -> sessions.createSession(savedTerminalSpec(saved)).id }
    layout.selectedTerminal?.let(restored::getOrNull)?.let(sessions::selectSession)
}
