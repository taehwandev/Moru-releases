package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.panel.PanelNode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.terminal.SessionClock
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.LimitReached
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.canAddTaskTerminalSession
import app.thdev.moru.feature.workbench.i18n.workbenchStrings
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.launch.NewSessionPopover
import app.thdev.moru.feature.workbench.resume.AgentExitBar
import app.thdev.moru.feature.workbench.resume.CliRestartBar
import app.thdev.moru.feature.workbench.resume.CliRestartRequests
import app.thdev.moru.feature.workbench.resume.TaskThemeRestartBar
import app.thdev.moru.feature.workbench.resume.themeRestartTarget
import app.thdev.moru.feature.workbench.resume.restartUpdatedAgent
import app.thdev.moru.feature.workbench.resume.ResumeBar
import app.thdev.moru.feature.workbench.resume.rememberAgentExits
import app.thdev.moru.feature.workbench.resume.startResumeTerminal
import app.thdev.moru.feature.workbench.resume.taskResumeTargets
import app.thdev.moru.feature.workbench.slots.TerminalPaneSpec
import app.thdev.moru.feature.workbench.slots.WorkbenchTerminalSlots
import kotlinx.coroutines.launch

/** The Task terminal view and its transient chooser; the app host owns launching. */
@Composable
internal fun TaskTerminalPane(
    sessions: TerminalSessionManager,
    folder: String,
    shell: String,
    taskKey: String,
    taskName: String,
    fontFamily: String?,
    fontSize: Int,
    scrollbackLines: Int,
    onOpenExternalTerminal: (TerminalSessionSnapshot) -> Unit,
    taskAgent: AgentRuntime?,
    onStartSession: (AgentRuntime) -> String?,
    slots: WorkbenchTerminalSlots,
    modifier: Modifier,
    newSessionRequested: Boolean = false,
    onNewSessionRequestHandled: () -> Unit = {},
    /** How the terminals last shared the area; see [TerminalPaneSpec.split]. */
    split: PanelNode? = null,
    onSplitChange: (PanelNode) -> Unit = {},
    publishesSelection: Boolean = true,
    /** One of the Task's terminals was chosen or took the keyboard; see [TerminalPaneSpec.onActivate]. */
    onActivate: (sessionId: String) -> Unit = {},
) {
    var choosingSession by remember(taskKey) { mutableStateOf(false) }
    var launchError by remember(taskKey) { mutableStateOf<String?>(null) }
    LaunchedEffect(taskKey, newSessionRequested) {
        if (newSessionRequested) {
            launchError = null
            choosingSession = sessions.canAddTaskTerminalSession(taskKey)
            onNewSessionRequestHandled()
        }
    }
    Column(modifier) {
        // A Task that needs the user is told through the notification; the terminal itself shows what
        // is asked, so no card here pushes it down.
        val strings = workbenchStrings()
        val agents = rememberAgentExits(sessions, taskKey)
        val exit = agents.exits.firstOrNull()
        val nowMillis by SessionClock.nowMillis.collectAsState()
        val themeRestart = themeRestartTarget(sessions.listSessions(), taskKey, sessions.selectedSessionId())
        val restart = CliRestartRequests.firstAmong(
            sessions.listSessions().filter { session -> session.spec.attachedItemId == taskKey }.map { session -> session.id },
        )
        val scope = rememberCoroutineScope()
        if (restart != null) {
            val (sessionId, runtime) = restart
            CliRestartBar(
                runtime = runtime,
                onRestart = {
                    CliRestartRequests.clear(sessionId)
                    agents.acknowledge(sessionId)
                    scope.launch { restartUpdatedAgent(sessions, sessionId, slots.submitLine) }
                },
                onDismiss = { CliRestartRequests.clear(sessionId) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else if (exit != null) {
            AgentExitBar(
                exit = exit,
                onResume = {
                    exit.resumeCommand?.let { command ->
                        if (slots.submitLine(exit.sessionId, command)) {
                            sessions.selectSession(exit.sessionId)
                            agents.acknowledge(exit.sessionId)
                        }
                    }
                },
                onDismiss = { agents.acknowledge(exit.sessionId) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else if (themeRestart != null) {
            TaskThemeRestartBar(
                session = themeRestart,
                taskKey = taskKey,
                sessions = sessions,
                nowMillis = nowMillis,
                submitLine = slots.submitLine,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else if (agents.anyAgentRunning == false && sessions.canAddTaskTerminalSession(taskKey)) {
            ResumeBar(
                runtimes = taskResumeTargets(sessions.listSessions(), taskKey).map { it.runtime },
                preferred = taskAgent,
                onResume = { runtime ->
                    startResumeTerminal(sessions, taskKey, folder, runtime, strings.resumeSessionTitle(runtime.displayName))
                },
            )
        }
        Box(Modifier.weight(1f)) {
            slots.pane(
                TerminalPaneSpec(
                    sessionManager = sessions,
                    defaultWorkingDirectory = folder,
                    defaultShell = shell,
                    workspaceId = taskKey,
                    workspaceName = taskName,
                    terminalFontFamily = fontFamily,
                    terminalFontSize = fontSize,
                    terminalScrollbackLines = scrollbackLines,
                    onOpenExternalTerminal = onOpenExternalTerminal,
                    onNewTerminal = {
                        if (sessions.canAddTaskTerminalSession(taskKey)) {
                            launchError = null
                            choosingSession = true
                        }
                    },
                    sessionBranch = slots.branchOf,
                    split = split,
                    onSplitChange = onSplitChange,
                    publishesSelection = publishesSelection,
                    onActivate = onActivate,
                ),
                Modifier.fillMaxSize(),
            )
            // A sliver standing where the tab bar's + button sits, so the chooser drops from that button while the
            // terminals stay in view; ⌘T opens it from the same place.
            Box(Modifier.align(Alignment.TopEnd).padding(end = 8.dp).size(width = 1.dp, height = 26.dp)) {
                NewSessionPopover(
                    expanded = choosingSession,
                    errorMessage = launchError,
                    onStart = { runtime ->
                        launchError = if (sessions.canAddTaskTerminalSession(taskKey)) onStartSession(runtime)
                        else LimitReached(taskKey).message
                        if (launchError == null) choosingSession = false
                    },
                    onDismiss = { choosingSession = false; launchError = null },
                    // It drops from the + at the end of the tab bar, so it opens towards this Task's terminals, never
                    // past them over the Task beside.
                    alignEnd = true,
                )
            }
        }
    }
}
