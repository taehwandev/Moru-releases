package app.thdev.moru.app

import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState

internal fun notificationTaskAvailable(notification: TaskNotification, sidebar: ProjectTaskSidebarState): Boolean =
    sidebar.projects.any { project ->
        project.id == notification.projectId && project.tasks.any { it.id == notification.taskId }
    }
