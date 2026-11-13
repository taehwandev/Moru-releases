package app.thdev.moru.app

import app.thdev.moru.core.model.AgentConversationRef

import app.thdev.moru.feature.workbench.*
import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.feature.codepanel.CodePanelSource
import app.thdev.moru.feature.workbench.slots.WorkbenchPaneSlots
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.domain.GitRepository
import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/** Desktop operations supplied to the workbench host, outside feature UI components. */
internal data class MoruWorkbenchBindings(
    val pickProjectFolder: () -> String?,
    val selectProjectRoot: (String) -> Unit,
    val viewFolderChanged: (String) -> Unit,
    val treeEntries: List<WorkbenchProjectTreeEntry>,
    val gitStatus: WorkbenchGitStatus,
    val refreshGitStatus: () -> Unit,
    val loadGitHistory: (String) -> GitHistoryResult,
    val loadCommitDiff: (String, String) -> CommitDiffResult,
    val loadWorkingChanges: (String) -> CommitDiffResult,
    val openFile: (String) -> WorkbenchFilePreview,
    val saveFile: (String, String) -> WorkbenchFileSaveOutcome,
    val createFile: (String) -> WorkbenchFileCreateOutcome,
    val renameFile: (String, String) -> WorkbenchFileRenameOutcome,
    val deleteFile: (String) -> WorkbenchFileDeleteOutcome,
    val refreshTree: () -> Unit,
    val dataRoot: String?,
    val startTerminal: (TerminalSessionSnapshot, AppSettings, MoruColors) -> Unit,
    val gitRepository: GitRepository? = null,
    /** Keys of the terminals the terminal host still holds from an earlier launch, or null when that is unknown. */
    val heldTerminalKeys: () -> Set<String>? = { null },
    val codePanelSource: CodePanelSource? = null,
    /** The terminal and preview views the workbench draws; other features supply them through the app. */
    val paneSlots: WorkbenchPaneSlots = DesktopWorkbenchPaneSlots,
    /** The children of a folder the tree scan deferred. */
    val loadFolder: (String) -> List<WorkbenchProjectTreeEntry> = { emptyList() },
    /**
     * Ends the terminal host's program under a host key, off the UI thread, even if Moru never attached to it this run;
     * a key the host does not hold is ignored.
     */
    val terminateHeldTerminal: (hostKey: String) -> Unit = {},
    /** The conversation the agent in terminal [sessionId] holds right now, so a closed terminal can resume it. Blocking. */
    val readAgentConversation: (sessionId: String) -> AgentConversationRef? = { null },
    /** The system folder chooser with a title, opening in a given folder when one is passed; null on cancel. */
    val pickFolder: (title: String, startIn: String?) -> String? = { _, _ -> null },
) {
    fun matchesFolder(folder: String): Boolean = dataRoot == null || dataRoot == folder
}
