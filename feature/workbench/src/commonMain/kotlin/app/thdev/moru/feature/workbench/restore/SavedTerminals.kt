package app.thdev.moru.feature.workbench.restore

import app.thdev.moru.core.model.SavedTerminal
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionStatus

/**
 * The terminals to bring back after a restart, in the order they were opened. A running one keeps its name in the
 * terminal host so Moru reattaches to it; one that already ended comes back as a new shell. A terminal whose link to
 * the host broke ended with no exit code, though its program may still run there, so it keeps its host name and
 * agent resume command too: the next launch reattaches to that program, or resumes its conversation if the host
 * no longer holds it.
 */
fun savedTerminalsOf(sessions: List<TerminalSessionSnapshot>): List<SavedTerminal> =
    sessions.map { session ->
        // A Lab conversation is kept by its key whatever state it is in; it never runs anything to restart.
        if (session.spec.kind == TerminalSessionKind.LAB) {
            return@map SavedTerminal(
                title = session.spec.title,
                shell = session.spec.shell,
                workingDirectory = session.spec.workingDirectory,
                taskId = session.spec.attachedItemId,
                startupCommand = session.spec.startupCommand,
                hostKey = session.spec.hostKey,
                lab = true,
            )
        }
        val ended = session.status == TerminalSessionStatus.EXITED || session.status == TerminalSessionStatus.FAILED
        val mayStillRun = session.status == TerminalSessionStatus.EXITED && session.exitCode == null
        SavedTerminal(
            title = session.spec.title,
            webPreview = session.spec.kind == TerminalSessionKind.WEB_PREVIEW,
            shell = session.spec.shell,
            workingDirectory = session.spec.workingDirectory,
            taskId = session.spec.attachedItemId,
            url = session.spec.url,
            startupCommand = if (ended && !mayStillRun) null else terminalRestoreCommand(session.spec.startupCommand),
            hostKey = if (ended && !mayStillRun) null else session.spec.hostKey,
        )
    }
