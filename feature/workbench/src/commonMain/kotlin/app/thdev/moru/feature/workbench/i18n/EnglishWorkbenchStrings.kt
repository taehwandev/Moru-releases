package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.feature.workbench.SidebarView
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24 * 60L
private const val WEEK_MINUTES = 7 * 24 * 60
private const val DAY_MINUTES = 24 * 60

internal object EnglishWorkbenchStrings : WorkbenchStrings {
    override val workspaceTitle = "Workspace"
    override val openProjectButton = "Open project…"
    override val reorderSidebarButton = "Reorder projects and tasks"
    override val finishReorderSidebarButton = "Done reordering"
    override fun sidebarViewName(view: SidebarView) = when (view) {
        SidebarView.Projects -> "Projects"
        SidebarView.Board -> "Status"
        SidebarView.Results -> "Moru"
    }
    override fun sidebarViewSwitcher(current: String) = "Sidebar view: $current"
    override fun closedProjects(count: Int) = "Closed projects · $count"
    override fun reopenProject(name: String) = "Reopen $name"
    override fun forgetClosedProject(name: String) = "Delete $name from the list"
    override val forgetClosedWarning = "Delete this project and its tasks from the list? Its folder and files are not touched."
    override val confirmForgetClosed = "Delete from list"
    override val keepClosed = "Keep"
    override val newTaskLabel = "New task"
    override fun newTaskIn(name: String) = "New task in $name"
    override val emptyTitle = "Your work starts with a project"
    override val emptyBody = "Open a folder using the button above. Its tasks and results will stay together here."
    override val noTasksYet = "No tasks yet. Start one in your selected project."

    override fun collapseProject(name: String) = "Collapse $name"
    override fun expandProject(name: String) = "Expand $name"
    override fun closeProject(name: String) = "Close $name"
    override fun projectMenu(name: String) = "Actions for $name"
    override fun taskMenu(title: String) = "Actions for $title"
    override val renameTask = "Rename"
    override val changeProjectFolder = "Change folder…"
    override val deleteTask = "Delete"
    override val openBeside = "Open beside"
    override val closeBeside = "Close beside"
    override val openInNewWindow = "Open in New Window"
    override val resizeBeside = "Resize Tasks beside"
    override val switchToTask = "Switch to this Task"
    override val deleteTaskWarning = "Delete this task? Its terminals stop and it leaves the list. Your files are not touched."
    override val confirmDeleteTask = "Delete task"
    override val keepTask = "Keep"
    override fun renameProject(name: String) = "Rename $name"
    override val saveProjectName = "Save name"
    override val cancelRename = "Cancel renaming"
    override val resumeLabel = "Resume last session"
    override fun agentExited(agent: String) = "$agent has quit."
    override val resumeHere = "Resume here"
    override val dismiss = "Dismiss"
    override fun agentUpdated(agent: String) = "$agent was updated. Restart to use the new version."
    override val restartAgent = "Restart"
    override fun codexThemeChanged(title: String, sessionId: String) = "Codex theme changed · $title ($sessionId)"
    override fun codexThemeRestartDetails(knownConversation: Boolean) =
        if (knownConversation) "Restart to apply the theme and resume this conversation in the same terminal. Unsubmitted input may be lost."
        else "Restart to apply the theme. Codex will open its conversation picker in this terminal. Unsubmitted input may be lost."
    override val themeRestartBusy = "Wait until Codex finishes working or asking for input."
    override val themeRestartInProgress = "Restarting Codex…"
    override val themeRestartFailed = "Codex could not restart safely. Try again when this session is idle."
    override val restartForTheme = "Restart for theme"
    override fun resumeSessionTitle(agent: String) = "$agent · resumed"
    override fun closeStopsRunning(count: Int) = if (count == 1) "1 agent is still running. Closing stops it." else "$count agents are still running. Closing stops them."
    override val closeAndStop = "Stop and close"
    override val keepOpen = "Keep open"
    override val closeTerminalPrompt = "Anything running in it stops."
    override fun pressAgainToClose(shortcut: String) = "$shortcut again to close"
    override fun closeTerminalTitle(title: String?) = if (title.isNullOrBlank()) "Close this terminal?" else "Close the $title terminal?"
    override fun reopenAfterClose(shortcut: String) = "$shortcut reopens it"
    override fun projectDescription(name: String) = "Project $name"
    override fun reorderProject(name: String) = "Reorder project $name. Drag or use Alt+Up or Alt+Down."
    override fun reorderTask(title: String) = "Reorder task $title. Drag or use Alt+Up or Alt+Down."
    override val moveSidebarItemUp = "Move up"
    override val moveSidebarItemDown = "Move down"
    override val folderUnavailable = "Folder unavailable"
    override val noFolderLinked = "No folder linked"
    override fun tasksNeedYou(count: Int, project: String) = "$count tasks need you in $project"
    override fun taskDescription(title: String) = "Task $title"
    override fun taskSessions(total: Int, working: Int, needsYou: Int) = buildList {
        add(if (total == 1) "1 session" else "$total sessions")
        if (working > 0) add("$working active")
        if (needsYou > 0) add("$needsYou need you")
    }.joinToString(" · ")
    override val terminalLabel = "Terminal"
    override fun sessionRunning(count: Int) = "Running $count"
    override fun sessionNeedsAttention(count: Int) = "Needs attention $count"
    override fun sessionResponsesReady(count: Int) = "Response ready $count"
    override fun finished(count: Int?) = if (count == null) "Done" else "Done $count"
    override fun sessionAwaitingReply(count: Int) = "Awaiting reply $count"
    override fun awaitingReply(count: Int?) = if (count == null) "Your reply" else "Your reply $count"
    override fun sessionWaiting(count: Int) = "Waiting $count"
    override fun sessionEnded(count: Int) = "Ended $count"

    override fun statusLabel(state: TaskDisplayState) = when (state) {
        TaskDisplayState.Understanding -> "Understanding"
        TaskDisplayState.Preparing -> "Preparing"
        TaskDisplayState.Working -> "Working"
        TaskDisplayState.Checking -> "Checking"
        TaskDisplayState.NeedsInput -> "Needs input"
        TaskDisplayState.NeedsApproval -> "Needs approval"
        TaskDisplayState.Done -> "Done"
        TaskDisplayState.Waiting -> "Waiting"
        TaskDisplayState.Stopped -> "Stopped"
        TaskDisplayState.Failed -> "Failed"
        TaskDisplayState.Unknown -> "Status unknown"
    }

    override fun sectionLabel(group: TaskGroup) = when (group) {
        TaskGroup.NeedsAttention -> "Needs you"
        TaskGroup.Running -> "In progress"
        TaskGroup.Review -> "Stopped"
        TaskGroup.Done -> "Finished"
    }

    override fun waiting(count: Int) = "$count waiting"
    override fun working(count: Int) = "$count working"
    override val noActiveTasks = "No active tasks"
}
