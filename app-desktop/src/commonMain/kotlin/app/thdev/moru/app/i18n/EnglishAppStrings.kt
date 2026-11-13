package app.thdev.moru.app.i18n

import app.thdev.moru.app.ProjectTab
import app.thdev.moru.app.MoruDestination

internal object EnglishAppStrings : AppStrings {
    override fun destinationLabel(destination: MoruDestination) = when (destination) {
        MoruDestination.Workbench -> "Workbench"
        MoruDestination.Knowledge -> "Knowledge"
        MoruDestination.Notifications -> "Notifications"
        MoruDestination.Settings -> "Settings"
    }

    override fun tabLabel(tab: ProjectTab) = when (tab) {
        ProjectTab.Work -> "Work"
        ProjectTab.Code -> "Code"
        ProjectTab.Git -> "Git"
    }

    override val selectProject = "Select a project"
    override val newTask = "New task"
    override val menuFile = "File"
    override val menuView = "View"
    override val menuProject = "Project"
    override val menuOpenProject = "Open Project…"
    override val menuNewTask = "New Task"
    override val menuCloseProject = "Close Project"
    override val menuQuitEndingTerminals = "Quit and End All Terminals"
    override val menuNewSession = "New Session"
    override val menuSplitRight = "Split Terminal Right"
    override val menuSplitDown = "Split Terminal Down"
    override val menuCloseTerminal = "Close Terminal"
    override val menuReopenClosedTerminal = "Reopen Closed Terminal"
    override val menuWork = "Work"
    override val menuCode = "Code"
    override val menuGit = "Git"
    override val menuSettings = "Settings"
    override val menuNextTask = "Next Task"
    override val menuPreviousTask = "Previous Task"
    override val menuNextSession = "Next Session"
    override val menuPreviousSession = "Previous Session"
    override val menuNextProject = "Next Project"
    override val menuPreviousProject = "Previous Project"
    override val menuKnowledge = "Knowledge"
    override val menuCheckForUpdates = "Check for Updates…"
    override val notificationTitle = "Moru"
    override fun attentionFailed(task: String) = "$task failed."
    override fun attentionAsking(task: String, agent: String?, question: String) =
        (if (agent != null) "$agent in $task is asking you" else "$task is asking you") +
            (if (question.isNotEmpty()) ": $question" else ".")
    override fun attentionFinished(task: String, agent: String) = "$agent in $task finished. Have a look at the result."
    override fun closeNeedsConfirm(count: Int) =
        if (count == 1) "1 agent is still running. Use the close mark in the sidebar to stop it and close."
        else "$count agents are still running. Use the close mark in the sidebar to stop them and close."
    override val quitTitle = "Quit Moru?"
    override fun quitEndsTerminals(count: Int) =
        (if (count == 1) "1 terminal is running" else "$count terminals are running") +
            ". Remember Last State is off, so quitting ends every terminal and the agents in them."
    override val quitKeepTerminals = "Keep the terminals running this time"
    override val quitCancel = "Cancel"
    override val quitConfirm = "Quit"
    override val startupFailedTitle = "Moru could not start"
    override val startupReportIssue = "Report Issue…"
    override val startupClose = "Close"
    override val crashTitle = "Moru ran into a problem"
    override fun crashBody(reason: String, log: String) =
        "Something went wrong that Moru did not handle: $reason\n\nYou can keep working, but if Moru misbehaves, " +
            "restart it. Reporting it helps fix it; details are in $log."
    override val crashLastTimeTitle = "Moru quit unexpectedly"
    override fun crashLastTime(reason: String, log: String) =
        "Moru ended last time because of an error: $reason\n\nReporting it helps fix it; details are in $log."
    override fun startupFailed(reason: String, log: String) =
        "Moru could not start: $reason\n\nDetails are in $log."
    override fun startupNeedsNewerMoru(log: String) =
        "Your Projects and Tasks were saved by a newer version of Moru, so this version cannot open them. " +
            "Install the latest Moru to keep using them; nothing was deleted.\n\nDetails are in $log."

    override val noProjectSelected = "No project selected"
    override fun readyForTask(project: String) = "$project › Ready for a task"
    override fun branchDetail(branch: String, place: String) = "Branch $branch · $place"
    override fun folderDetail(place: String) = "Project folder · $place"

    override val welcomeEmptyTitle = "A place for your next task"
    override val welcomeEmptyBody = "Open a folder, describe the work, and keep the agent and its changes together."
    override val welcomeProjectBody = "Describe what you want to change. Your task, terminal, and diff will stay together here."
    override val openProject = "Open project"
    override val terminalHint = "From a terminal: Moru --project /path/to/project"
    override val unavailableTitle = "Project folder unavailable"
    override fun unavailableBody(name: String) =
        "The folder for $name cannot be accessed. If you renamed or moved it, find it to keep its tasks."
    override val findProjectFolder = "Find folder"
    override val findProjectFolderTitle = "Choose the project's folder"
    override val projectFolderInUse = "That folder is already another project. Close and delete that one first to move this project there."
    override val projectFolderUnavailable = "That folder cannot be accessed."
    override fun projectInOwnWindowTitle(name: String) = "$name is open in its own window"
    override val projectInOwnWindowBody = "Its terminals and Result are in that window."
    override val bringWindowForward = "Bring Window Forward"
    override val moveToMainWindow = "Move to Main Window"

    override val preparingTask = "Preparing your task…"
    override val taskCouldNotStart = "Task could not start"
    override val noAgentLaunched = "No agent was launched. Retry preparation or explicitly use the project folder."
    override val retry = "Retry"
    override val switchWhilePreparing = "You can switch tasks while the workspace is prepared."
    override val taskBrief = "Task brief"
    override val hideTaskBrief = "Hide task brief"

    override val dismiss = "Dismiss"
    override val loadingProjectFiles = "Loading project files…"

    override val updateChecking = "Checking for updates…"
    override fun updateUpToDate(version: String) = "Moru $version is the latest version."
    override fun updateDownloading(version: String, percent: Int) = "Downloading Moru $version… $percent%"
    override fun updateReady(version: String) = "Moru $version is ready."
    override val updateRestart = "Restart to Update"
    override val updateLater = "Later"
    override fun updateInstalling(version: String) = "Installing Moru $version…"
    override fun updateFailed(message: String) = "Could not update Moru: $message"
    override val unknownError = "Unknown error"
    override fun couldNotChooseFolder(message: String) = "Could not choose folder: $message"
    override fun couldNotOpenProject(message: String) = "Could not open project: $message"
    override val chooseWorkflowFolder = "Choose a workflow folder"
    override fun couldNotStart(title: String, message: String) = "Could not start \"$title\": $message"
}
