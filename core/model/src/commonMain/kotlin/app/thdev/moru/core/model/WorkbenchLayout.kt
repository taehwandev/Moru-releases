package app.thdev.moru.core.model

import app.thdev.moru.core.model.panel.PanelNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the user left open in the workbench, so the next launch shows it again: the terminals of every Task (in the
 * order they were opened), which one was chosen, each Task's Result pane and Code tab, the Projects the user collapsed
 * (every other one is expanded), the file open in each folder's Code view, the terminals closed with Cmd+W that can be
 * opened again. It is
 * layout, not content: a terminal comes back as a new process. The older `expanded_projects` list is ignored when read.
 */
@Serializable
data class WorkbenchLayout(
    @SerialName("collapsed_projects") val collapsedProjectIds: List<String> = emptyList(),
    val terminals: List<SavedTerminal> = emptyList(),
    /** Index into [terminals] of the terminal that was chosen, or null. */
    @SerialName("selected_terminal") val selectedTerminal: Int? = null,
    /** Terminals the user closed with Cmd+W, oldest first, so the newest one opens again first. */
    @SerialName("closed_terminals") val closedTerminals: List<SavedTerminal> = emptyList(),
    /** By Task id. */
    @SerialName("result_panes") val resultPanes: Map<String, SavedResultPane> = emptyMap(),
    /** The Result width share the user last dragged, for a Task without its own; null is half. */
    @SerialName("result_share") val resultShare: Float? = null,
    /** By Task id. */
    @SerialName("code_panels") val codePanels: Map<String, SavedCodePanel> = emptyMap(),
    /** The open file's path by the folder the Code view showed. */
    @SerialName("open_files") val openFiles: Map<String, String> = emptyMap(),
    /** The folders open in each folder's Code explorer, by the folder the Code view showed. */
    @SerialName("expanded_folders") val expandedFolders: Map<String, List<String>> = emptyMap(),
    /** The Code explorer's options, the same in every folder. */
    @SerialName("code_explorer") val codeExplorer: CodeExplorerOptions = CodeExplorerOptions(),
    /** The Git detail pane width in dp the user dragged it to; null follows the window. */
    @SerialName("git_detail_width") val gitDetailWidthDp: Float? = null,
    /** The Settings page the user last opened, by its name, or null for the first. */
    @SerialName("settings_page") val settingsPage: String? = null,
    /** How each Task's terminals share its terminal area, by Task id; a Task without one splits it evenly. */
    @SerialName("terminal_splits") val terminalSplits: Map<String, PanelNode> = emptyMap(),
    /** Tasks whose terminals the user opened beside the selected Task, in the order they were opened. */
    @SerialName("beside_tasks") val besideTaskIds: List<String> = emptyList(),
    /** How the selected Task and the Tasks beside it share the width; null is even. */
    @SerialName("beside_split") val besideSplit: PanelNode? = null,
    /** Projects open in windows of their own, in the order they were opened, with each window's place. */
    @SerialName("project_windows") val projectWindows: List<SavedProjectWindow> = emptyList(),
    /** The left sidebar's view by its name (the Project tree or the status board), or null for the tree. */
    @SerialName("sidebar_view") val sidebarView: String? = null,
    /** The results the user marked seen in the Moru view, by result, newest last, so they stay folded on the next launch. */
    @SerialName("seen_results") val seenResults: List<Int> = emptyList(),
)
