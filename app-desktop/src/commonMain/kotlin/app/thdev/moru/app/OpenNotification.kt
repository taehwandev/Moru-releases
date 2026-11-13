package app.thdev.moru.app

import app.thdev.moru.core.model.TaskNotification
/** Resolve the current catalog before navigation; receipt snapshots carry no authority. */
internal fun openNotification(notification: TaskNotification, navigation: WorkbenchNavigation): Boolean {
    if (!notificationTaskAvailable(notification, navigation.sidebar)) return false
    navigation.selectTask(notification.projectId, notification.taskId)
    // A notification is about a session, so it opens on the Task's terminals whatever view was open.
    navigation.tab = ProjectTab.Work
    return navigation.sidebar.selectedProjectId == notification.projectId &&
        navigation.sidebar.selectedTaskId == notification.taskId
}
