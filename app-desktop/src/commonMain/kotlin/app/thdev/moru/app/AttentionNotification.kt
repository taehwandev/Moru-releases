package app.thdev.moru.app

import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState

/** Capture receipt text/names now; opening it later must resolve current catalog membership. */
internal fun attentionNotification(
    candidate: AttentionCandidate,
    sidebar: ProjectTaskSidebarState,
    message: String,
    id: String,
    now: Long,
): TaskNotification? {
    val project = sidebar.projects.firstOrNull { project -> project.tasks.any { it.id == candidate.taskId } } ?: return null
    val sessionId = candidate.sessionId.takeIf { candidate.kind == AttentionKind.Asking }
    val requestId = candidate.permissionRequestId?.takeIf { expected -> sessionId?.let { SessionPermissions.requests.value[it]?.id } == expected }
    NotificationPermissionBindings.Current.capture(id, sessionId, requestId)
    return TaskNotification(id, project.id, candidate.taskId, project.displayName, candidate.title, message, now)
}
