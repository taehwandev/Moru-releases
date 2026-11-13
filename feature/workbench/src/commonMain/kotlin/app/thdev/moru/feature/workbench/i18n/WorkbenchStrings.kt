package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.feature.workbench.SidebarView
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup

/**
 * The workbench's own text in one language. Every screen reads it through [workbenchStrings], so a
 * language is complete only when it implements every member: a missing one fails compilation.
 * Names of agents and products ("Claude") are not translated and live in the screens.
 */
internal interface WorkbenchStrings {
    // Sidebar
    val workspaceTitle: String
    val openProjectButton: String
    val reorderSidebarButton: String
    val finishReorderSidebarButton: String

    /** The sidebar view picker: each view's name, and the picker described with the view it shows. */
    fun sidebarViewName(view: SidebarView): String
    fun sidebarViewSwitcher(current: String): String
    fun closedProjects(count: Int): String
    fun reopenProject(name: String): String
    fun forgetClosedProject(name: String): String
    val forgetClosedWarning: String
    val confirmForgetClosed: String
    val keepClosed: String

    /** The add-a-Task row at the foot of a Project group: its visible label, and its name for the Project it adds to. */
    val newTaskLabel: String
    fun newTaskIn(name: String): String
    val emptyTitle: String
    val emptyBody: String
    val noTasksYet: String

    // Project and Task rows
    fun collapseProject(name: String): String
    fun expandProject(name: String): String
    fun projectDescription(name: String): String
    fun reorderProject(name: String): String
    fun reorderTask(title: String): String
    val moveSidebarItemUp: String
    val moveSidebarItemDown: String
    fun closeProject(name: String): String
    fun projectMenu(name: String): String
    fun taskMenu(title: String): String
    val renameTask: String
    val changeProjectFolder: String
    val deleteTask: String
    val openBeside: String
    val closeBeside: String
    val openInNewWindow: String
    val resizeBeside: String
    val switchToTask: String
    val deleteTaskWarning: String
    val confirmDeleteTask: String
    val keepTask: String
    fun renameProject(name: String): String
    val saveProjectName: String
    val cancelRename: String
    val resumeLabel: String
    fun agentExited(agent: String): String
    val resumeHere: String
    val dismiss: String
    fun agentUpdated(agent: String): String
    val restartAgent: String
    fun codexThemeChanged(title: String, sessionId: String): String
    fun codexThemeRestartDetails(knownConversation: Boolean): String
    val themeRestartBusy: String
    val themeRestartInProgress: String
    val themeRestartFailed: String
    val restartForTheme: String
    fun resumeSessionTitle(agent: String): String
    fun closeStopsRunning(count: Int): String
    val closeAndStop: String
    val keepOpen: String
    val closeTerminalPrompt: String
    fun pressAgainToClose(shortcut: String): String
    /** The alert's question, naming the terminal when it has a [title]. */
    fun closeTerminalTitle(title: String?): String
    fun reopenAfterClose(shortcut: String): String
    val folderUnavailable: String
    val noFolderLinked: String
    fun tasksNeedYou(count: Int, project: String): String
    fun taskDescription(title: String): String
    fun taskSessions(total: Int, working: Int, needsYou: Int): String
    fun sessionRunning(count: Int): String
    fun sessionNeedsAttention(count: Int): String
    fun sessionResponsesReady(count: Int): String
    /** The green finished pill: with how many sessions finished on a Project, without a count on a Task. */
    fun finished(count: Int?): String
    /** Sessions whose agent ended its turn with a question; the amber pill on a Project (with a count) or a Task. */
    fun sessionAwaitingReply(count: Int): String
    fun awaitingReply(count: Int?): String
    fun sessionWaiting(count: Int): String
    fun sessionEnded(count: Int): String
    val terminalLabel: String
    fun statusLabel(state: TaskDisplayState): String
    fun sectionLabel(group: TaskGroup): String

    // Status bar
    fun waiting(count: Int): String
    fun working(count: Int): String
    val noActiveTasks: String
}
