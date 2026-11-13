package app.thdev.moru.app

import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState
import app.thdev.moru.feature.workbench.TaskRow

/** Catalog validation and navigation for an existing session; the registry owns its atomic attachment mutation. */
internal object SessionTaskMove {
    fun targets(sidebar: ProjectTaskSidebarState, session: TerminalSessionSnapshot?): List<TaskRow> {
        if (session == null || !session.spec.kind.isTaskSession) return emptyList()
        val project = sidebar.projects.firstOrNull { project ->
            project.tasks.any { it.id == session.spec.attachedItemId }
        }?.takeIf { it.isAvailable } ?: return emptyList()
        return project.tasks.filter { it.id != session.spec.attachedItemId }
    }

    fun move(navigation: WorkbenchNavigation, sessions: TerminalSessionManager, sessionId: String, sourceTaskId: String?, taskId: String): Result<Unit> =
        runCatching {
            navigation.reload()
            val session = sessions.listSessions().firstOrNull { it.id == sessionId }
                ?: error("This session is no longer available.")
            check(session.spec.attachedItemId == sourceTaskId) { "This session has moved to another Task. Open its menu again." }
            val target = targets(navigation.sidebar, session).firstOrNull { it.id == taskId }
                ?: error("Choose an available Task in the same Project. The Task list may have changed.")
            check(sessions.moveSessionToTask(sessionId, target.id)) { "This session could not be moved. Try again." }
            navigation.selectTask(target.projectId, target.id)
            navigation.tab = ProjectTab.Work
            sessions.selectSession(sessionId)
        }
}
