package app.thdev.moru.feature.workbench.slots

import app.thdev.moru.core.designsystem.component.MoruPanelCell
import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/** One Task's terminals as the terminal pane slot shows them: where new ones start, how they look, what + does. */
data class TerminalPaneSpec(
    val sessionManager: TerminalSessionManager,
    val defaultWorkingDirectory: String,
    val defaultShell: String,
    val workspaceId: String,
    val workspaceName: String,
    val terminalFontFamily: String?,
    val terminalFontSize: Int,
    val terminalScrollbackLines: Int,
    val onOpenExternalTerminal: (TerminalSessionSnapshot) -> Unit,
    val onNewTerminal: (() -> Unit)?,
    /** Names each terminal's branch for its header; see [WorkbenchTerminalSlots.branchOf]. */
    val sessionBranch: SessionBranchReader? = null,
    /** How the Task's terminals last shared the area, or null for an even split. */
    val split: PanelNode? = null,
    /** The user moved or reset a border between the Task's terminals. */
    val onSplitChange: (PanelNode) -> Unit = {},
    /** Whether choosing one of these terminals makes it the app's selected session; off for a Task shown beside. */
    val publishesSelection: Boolean = true,
    /** Panels arranged among the terminals, such as the Task's devices put there. */
    val panels: List<MoruPanelCell> = emptyList(),
    /** One of these terminals was chosen or took the keyboard; the window's shortcuts then act on its Task. */
    val onActivate: (sessionId: String) -> Unit = {},
    /** Whether + can offer panels independently of the Task's terminal-session capacity. */
    val canOpenPanels: Boolean = false,
)
