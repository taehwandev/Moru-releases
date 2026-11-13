package app.thdev.moru.feature.notifications

import app.thdev.moru.core.model.TaskNotification

internal object NotificationInboxFixtures {
    val newest = TaskNotification(
        id = "new",
        projectId = "project-a",
        taskId = "task-a",
        projectName = "Alpha",
        taskTitle = "Fix sign in",
        message = "The agent is asking you: Keep the existing sign-in screen?",
        createdAtMillis = 1_700_000_100_000L,
    )
    val older = TaskNotification(
        id = "old",
        projectId = "project-b",
        taskId = "task-b",
        projectName = "Beta",
        taskTitle = "Review result",
        message = "The agent finished. Have a look at the result.",
        createdAtMillis = 1_700_000_000_000L,
        read = true,
    )
    val populated = NotificationInboxUiState(loading = false, notifications = listOf(newest, older))
    val longNames = newest.copy(
        projectName = "A project with a long name · 로컬 프로젝트 · release workspace",
        taskTitle = "Check the sign-in flow and review the complete changes before continuing with the next task",
        message = "The agent is asking you: Should the sign-in screen keep its current behavior for returning users? " +
            "Please review the proposed result before the task continues. 이 알림은 원래 메시지를 그대로 표시합니다.",
    )
}
