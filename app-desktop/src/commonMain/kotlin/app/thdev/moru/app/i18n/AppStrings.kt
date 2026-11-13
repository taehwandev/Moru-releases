package app.thdev.moru.app.i18n

import app.thdev.moru.app.ProjectTab
import app.thdev.moru.app.MoruDestination

/**
 * The app shell's own text in one language: tabs, the context line, the welcome and launch screens, the
 * notice banner and the messages shown when opening or starting something fails. A language is complete
 * only when it implements every member. Product names ("Moru") are not translated.
 */
internal interface AppStrings {
    fun destinationLabel(destination: MoruDestination): String
    fun tabLabel(tab: ProjectTab): String
    val selectProject: String
    val menuFile: String
    val menuView: String
    val menuProject: String
    val menuOpenProject: String
    val menuNewTask: String
    val menuCloseProject: String
    val menuQuitEndingTerminals: String
    val menuNewSession: String
    val menuSplitRight: String
    val menuSplitDown: String
    val menuCloseTerminal: String
    val menuReopenClosedTerminal: String
    val menuWork: String
    val menuCode: String
    val menuGit: String
    val menuSettings: String
    val menuNextTask: String
    val menuPreviousTask: String
    val menuNextSession: String
    val menuPreviousSession: String
    val menuNextProject: String
    val menuPreviousProject: String
    val menuKnowledge: String
    val menuCheckForUpdates: String
    fun closeNeedsConfirm(count: Int): String

    /** The question asked on quit while Remember Last State is off and terminals are running. */
    val quitTitle: String
    fun quitEndsTerminals(count: Int): String
    val quitKeepTerminals: String
    val quitCancel: String
    val quitConfirm: String

    /** Shown instead of the window when Moru cannot start; [log] is where the details were written. */
    val startupFailedTitle: String
    fun startupFailed(reason: String, log: String): String
    fun startupNeedsNewerMoru(log: String): String
    val startupReportIssue: String
    val startupClose: String

    /** Offered when an error nothing handled happened now, or ended Moru the last time; [log] holds its details. */
    val crashTitle: String
    fun crashBody(reason: String, log: String): String
    val crashLastTimeTitle: String
    fun crashLastTime(reason: String, log: String): String

    val notificationTitle: String
    fun attentionFailed(task: String): String
    fun attentionAsking(task: String, agent: String?, question: String): String
    fun attentionFinished(task: String, agent: String): String
    val newTask: String

    // Context line
    val noProjectSelected: String
    fun readyForTask(project: String): String
    fun branchDetail(branch: String, place: String): String
    fun folderDetail(place: String): String

    // Welcome and unavailable project
    val welcomeEmptyTitle: String
    val welcomeEmptyBody: String
    val welcomeProjectBody: String
    val openProject: String
    val terminalHint: String
    val unavailableTitle: String
    fun unavailableBody(name: String): String
    val findProjectFolder: String
    val findProjectFolderTitle: String
    val projectFolderInUse: String
    val projectFolderUnavailable: String
    fun projectInOwnWindowTitle(name: String): String
    val projectInOwnWindowBody: String
    val bringWindowForward: String
    val moveToMainWindow: String

    // Task launch
    val preparingTask: String
    val taskCouldNotStart: String
    val noAgentLaunched: String
    val retry: String
    val switchWhilePreparing: String
    val taskBrief: String
    val hideTaskBrief: String

    // Banner and loading
    val dismiss: String
    val loadingProjectFiles: String

    // Updates
    val updateChecking: String
    fun updateUpToDate(version: String): String
    fun updateDownloading(version: String, percent: Int): String
    fun updateReady(version: String): String
    val updateRestart: String
    val updateLater: String
    fun updateInstalling(version: String): String
    fun updateFailed(message: String): String

    // Failures
    val unknownError: String
    fun couldNotChooseFolder(message: String): String
    fun couldNotOpenProject(message: String): String
    /** The folder picker's title when the welcome imports a workflow folder. */
    val chooseWorkflowFolder: String
    fun couldNotStart(title: String, message: String): String
}
