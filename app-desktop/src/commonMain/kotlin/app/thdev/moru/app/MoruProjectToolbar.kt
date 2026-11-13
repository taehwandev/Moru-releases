package app.thdev.moru.app

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.feature.sessions.SessionsOverviewContent
import app.thdev.moru.feature.sessions.rememberAgentSessionRows
import app.thdev.moru.feature.terminal.rememberTerminalRevision
import app.thdev.moru.feature.terminal.requestTerminalFocus

/** Navigation and session effects for the window toolbar; all selection goes through the sidebar's owner. */
@Composable
internal fun MoruProjectToolbar(
    navigation: WorkbenchNavigation,
    bindings: MoruWorkbenchBindings,
    sessions: TerminalSessionManager,
    modifier: Modifier = Modifier,
) {
    val strings = appStrings()
    val tasks = navigation.sidebar.projects.flatMap { project ->
        project.tasks.map { it.id to SessionTaskRef(it.title, project.id, project.displayName) }
    }.toMap()
    ProjectHeader(navigation.sidebar, navigation.selectedTask?.title, navigation.tab,
        onSelect = { navigation.tab = it; navigation.cancelComposition() },
        onOpenProject = { navigation.openProjectFromPicker(bindings.pickProjectFolder, strings) },
        onSelectProject = navigation::selectProject,
        onReopenProject = navigation::openProject,
        modifier = modifier,
        sessions = { dismiss ->
            val rows = rememberAgentSessionRows(sessions, rememberTerminalRevision(sessions).value, scans = false)
            var thisProjectOnly by remember { mutableStateOf(false) }
            SessionsOverviewContent(rows, tasks, navigation.sidebar.selectedProjectId, thisProjectOnly,
                onScopeChange = { thisProjectOnly = it },
                onSelectSession = { row, projectId ->
                    dismiss()
                    sessions.selectSession(row.sessionId)
                    row.taskKey?.let { if (projectId != null) navigation.selectTask(projectId, it) }
                    navigation.tab = ProjectTab.Work
                    ViewedSessionResponseObserver.requestOpening(row.sessionId)
                    requestTerminalFocus(row.sessionId)
                },
                onOpenPreview = { _, _ -> })
        })
}
