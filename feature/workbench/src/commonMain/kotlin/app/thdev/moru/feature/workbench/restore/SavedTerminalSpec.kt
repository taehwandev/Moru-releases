package app.thdev.moru.feature.workbench.restore

import app.thdev.moru.core.model.SavedTerminal
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec

/**
 * The session [saved] describes, to create again after a restart or when a closed terminal is reopened.
 * Non-Lab startup commands are normalized here too, so older saved resume fallbacks cannot start a new conversation.
 */
fun savedTerminalSpec(saved: SavedTerminal): TerminalSessionSpec =
    TerminalSessionSpec(
        title = saved.title,
        kind = when {
            saved.lab -> TerminalSessionKind.LAB
            saved.webPreview -> TerminalSessionKind.WEB_PREVIEW
            else -> TerminalSessionKind.TERMINAL
        },
        shell = saved.shell,
        workingDirectory = saved.workingDirectory,
        attachedItemId = saved.taskId,
        url = saved.url,
        startupCommand = if (saved.lab) saved.startupCommand else terminalRestoreCommand(saved.startupCommand),
        hostKey = saved.hostKey,
    )
