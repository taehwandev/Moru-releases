package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.feature.workbench.beside.BesideTask
import app.thdev.moru.feature.workbench.result.ResultTaskPicker
import app.thdev.moru.feature.workbench.beside.BesideTaskPane
import app.thdev.moru.feature.workbench.beside.BesideTasksLayout
import app.thdev.moru.feature.workbench.slots.TerminalPaneSpec
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.domain.TerminalWorkspaceRepository
import app.thdev.moru.core.model.SavedResultPane
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.workbench.i18n.resultStrings
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.result.ResultPaneState
import app.thdev.moru.feature.workbench.result.ResultTab
import app.thdev.moru.feature.workbench.result.ResultToolStrip
import app.thdev.moru.feature.workbench.result.ResultWorkbenchLayout
import app.thdev.moru.feature.workbench.result.TaskResultPane
import app.thdev.moru.feature.workbench.slots.WorkbenchPaneSlots

/** The terminal (or another tab's [content]) and movable Result pane; tabs and folding are restored per Task. */
@Composable
fun WorkbenchScreen(
    workspacePath: String,
    terminalSessionManager: TerminalSessionManager,
    terminalWorkspaceRepository: TerminalWorkspaceRepository,
    onStartSession: (AgentRuntime) -> String?,
    /** The terminal and Code panel views drawn by other features, supplied by the app; no Code tab without one. */
    paneSlots: WorkbenchPaneSlots,
    defaultShell: String = "zsh",
    terminalFontFamily: String? = null,
    terminalFontSize: Int = 13,
    terminalScrollbackLines: Int = 5000,
    activeTerminalKey: String = "workbench",
    activeTerminalName: String = "Terminal",
    /** The selected Task's state, shown in its header while a Task is open beside it. */
    activeTaskState: TaskDisplayState? = null,
    /** Whether choosing one of the selected Task's terminals makes it the app's selected session; see [TerminalPaneSpec.publishesSelection]. */
    publishesSelection: Boolean = true,
    /** A terminal of the selected Task or of the Task beside was chosen or took the keyboard. */
    onTerminalActivated: (taskId: String, sessionId: String) -> Unit = { _, _ -> },
    /** Starts a session in the Task beside, returning an error to show; none leaves it without a chooser. */
    onStartBesideSession: ((taskId: String, AgentRuntime) -> String?)? = null,
    /** The Task beside whose chooser ⌘T asked to open. */
    besideNewSessionRequest: String? = null,
    onOpenExternalTerminal: (TerminalSessionSnapshot) -> Unit = {},
    /** Each Task's Result pane as the user last left it, by Task id; it outlives this screen and the app. */
    resultPanes: Map<String, SavedResultPane> = emptyMap(),
    /** Only the Tasks whose Result pane changed here, so another window's newer panes are not written over. */
    onResultPanesChange: (Map<String, SavedResultPane>) -> Unit = {},
    /** When this moves, [resultPanes] is read again, as when another window that changed them has closed. */
    resultPanesEpoch: Any? = null,
    /** The Result width share last dragged on any Task; a Task without its own width opens at it. */
    resultShare: Float? = null,
    onResultShareChange: (Float?) -> Unit = {},
    /** How this Task's terminals last shared the area, or null for an even split. */
    terminalSplit: PanelNode? = null,
    onTerminalSplitChange: (PanelNode) -> Unit = {},
    /** Other Tasks whose terminals are open beside this one; this Task itself is never drawn beside. */
    besideTasks: List<BesideTask> = emptyList(),
    /** How this Task and the Tasks beside it share the width; null is even. */
    besideSplit: PanelNode? = null,
    onBesideSplitChange: (PanelNode) -> Unit = {},
    onCloseBeside: (taskId: String) -> Unit = {},
    /** The user picked a Task beside: it becomes the selected Task and this one takes its place beside. */
    onSelectBeside: (taskId: String) -> Unit = {},
    /** How each Task's terminals last shared its own area, for the Tasks beside, by Task id. */
    besideTerminalSplits: Map<String, PanelNode> = emptyMap(),
    onBesideTerminalSplitChange: (taskId: String, PanelNode) -> Unit = { _, _ -> },
    taskAgent: AgentRuntime? = null,
    /** Cmd+T asked for another session in this Task; the terminal pane opens its chooser and reports it handled. */
    newSessionRequested: Boolean = false,
    onNewSessionRequestHandled: () -> Unit = {},
    /**
     * What another Project tab (Code, Git) draws in the terminal's place. The Result pane and its strip stay as they
     * are, so a tool left open follows the user across tabs.
     */
    content: (@Composable (Modifier) -> Unit)? = null,
) {
    val strings = resultStrings()
    val pane = remember(resultPanesEpoch) { ResultPaneState(resultPanes, ResultTab.Code, resultShare) }
    val panesChanged by rememberUpdatedState(onResultPanesChange)
    val shareChanged by rememberUpdatedState(onResultShareChange)
    LaunchedEffect(pane) {
        var last = pane.savedPanes
        snapshotFlow { pane.savedPanes }.collect { panes ->
            panes.filter { (task, saved) -> last[task] != saved }.takeIf { it.isNotEmpty() }?.let(panesChanged)
            last = panes
        }
    }
    LaunchedEffect(pane) { snapshotFlow { pane.lastShare }.collect { shareChanged(it) } }

    // The Result shows this Task's own, or that of a Task beside it the user picked in the Result header.
    val others = besideTasks.filter { it.id != activeTerminalKey }
    val shown = others.firstOrNull { it.id == pane.shownTask(activeTerminalKey) }
    val resultKey = shown?.id ?: activeTerminalKey
    val resultFolder = shown?.folder ?: workspacePath
    val available = buildSet { if (paneSlots.code != null) add(ResultTab.Code) }
    val selected = pane.selected(activeTerminalKey)

    ResultWorkbenchLayout(
        taskKey = activeTerminalKey,
        pane = pane,
        resultOpen = pane.isOpen(activeTerminalKey, available),
        dividerLabel = strings.resizeResult,
        resetLabel = strings.resetPaneSize,
        modifier = Modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page),
        detached = pane.detached,
        detachedTitle = strings.detachedTitle(activeTerminalName),
        onRestoreResult = { pane.restore(activeTerminalKey) },
        terminal = { modifier ->
            if (content != null) content(modifier) else BesideTasksLayout(
                tasks = besideTasks.filter { it.id != activeTerminalKey },
                split = besideSplit,
                onSplitChange = onBesideSplitChange,
                modifier = modifier,
                main = { mainModifier ->
                    TaskTerminalPane(
                        terminalSessionManager, workspacePath, defaultShell, activeTerminalKey, activeTerminalName,
                        terminalFontFamily, terminalFontSize, terminalScrollbackLines, onOpenExternalTerminal,
                        taskAgent = taskAgent,
                        onStartSession = onStartSession,
                        slots = paneSlots.terminal,
                        newSessionRequested = newSessionRequested,
                        onNewSessionRequestHandled = onNewSessionRequestHandled,
                        modifier = mainModifier,
                        split = terminalSplit,
                        onSplitChange = onTerminalSplitChange,
                        publishesSelection = publishesSelection,
                        onActivate = { id -> onTerminalActivated(activeTerminalKey, id) },
                    )
                },
                mainTitle = activeTerminalName,
                mainState = activeTaskState,
                onDropOnMain = onSelectBeside,
                beside = { task, besideModifier, headerModifier, dropHere ->
                    BesideTaskPane(
                        task = task,
                        spec = TerminalPaneSpec(
                            sessionManager = terminalSessionManager,
                            defaultWorkingDirectory = task.folder,
                            defaultShell = defaultShell,
                            workspaceId = task.id,
                            workspaceName = task.title,
                            terminalFontFamily = terminalFontFamily,
                            terminalFontSize = terminalFontSize,
                            terminalScrollbackLines = terminalScrollbackLines,
                            onOpenExternalTerminal = onOpenExternalTerminal,
                            onNewTerminal = null,
                            sessionBranch = paneSlots.terminal.branchOf,
                            split = besideTerminalSplits[task.id],
                            onSplitChange = { split -> onBesideTerminalSplitChange(task.id, split) },
                            // The selected Task keeps the app's selected session; this one only remembers its own.
                            publishesSelection = false,
                            onActivate = { id -> onTerminalActivated(task.id, id) },
                        ),
                        slots = paneSlots.terminal,
                        onClose = { onCloseBeside(task.id) },
                        onSelect = { onSelectBeside(task.id) },
                        permissionActions = paneSlots.sessions.permissionActions,
                        modifier = besideModifier,
                        headerModifier = headerModifier,
                        dropHere = dropHere,
                        // With a way to start sessions in it, the Task beside gets the same chooser and bars as the
                        // selected Task, so ⌘T and + there add to it.
                        terminals = onStartBesideSession?.let { start ->
                            { terminalsModifier ->
                                TaskTerminalPane(
                                    terminalSessionManager, task.folder, defaultShell, task.id, task.title,
                                    terminalFontFamily, terminalFontSize, terminalScrollbackLines, onOpenExternalTerminal,
                                    taskAgent = null,
                                    onStartSession = { runtime -> start(task.id, runtime) },
                                    slots = paneSlots.terminal,
                                    modifier = terminalsModifier,
                                    newSessionRequested = besideNewSessionRequest == task.id,
                                    onNewSessionRequestHandled = onNewSessionRequestHandled,
                                    split = besideTerminalSplits[task.id],
                                    onSplitChange = { split -> onBesideTerminalSplitChange(task.id, split) },
                                    publishesSelection = false,
                                    onActivate = { id -> onTerminalActivated(task.id, id) },
                                )
                            }
                        },
                    )
                },
            )
        },
        result = { modifier ->
            TaskResultPane(
                selected, resultKey, resultFolder, pane, terminalSessionManager, paneSlots,
                modifier = modifier,
                taskPicker = if (others.isEmpty()) null else {
                    {
                        ResultTaskPicker(
                            shown = resultKey,
                            tasks = listOf(activeTerminalKey to activeTerminalName) + others.map { it.id to it.title },
                            onPick = { id -> pane.showTask(activeTerminalKey, id) },
                        )
                    }
                },
            )
        },
        strip = { modifier ->
            ResultToolStrip(
                selected = selected,
                open = pane.isOpen(activeTerminalKey, available) && !pane.detached,
                available = available,
                onPick = { tab -> pane.pick(activeTerminalKey, tab, available) },
                modifier = modifier,
                detached = pane.detached,
                onDock = { pane.restore(activeTerminalKey) },
            )
        },
    )
}
