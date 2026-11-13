package app.thdev.moru.app.screenshot

import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity

/** Matches the ambiguous repeated Task titles from the reported sidebar, without live processes. */
internal object TaskGroupingFixtures {
    fun state() = AttentionFixtures.sidebarState().let { base ->
        val project = base.projects.first()
        val template = project.tasks.first()
        base.copy(
            projects = listOf(project.copy(
                displayName = "KeyFlow",
                tasks = (1..4).map { index -> template.copy(
                    id = "task-$index", selected = index == 2,
                    title = if (index == 4) "한글 입력과 a very long task title that must wrap inside the narrow sidebar" else "Terminal session",
                ) },
            )),
            selectedTaskId = "task-2",
        )
    }

    fun sessions() = listOf(
        session("s1", "task-1", "Terminal 1", AgentRuntime.Terminal, SessionActivity.Idle),
        session("s2", "task-1", "Terminal 2", AgentRuntime.Named("Claude"), SessionActivity.Working, "Editing auth/LoginViewModel.kt"),
        session("s3", "task-2", "Terminal 3", AgentRuntime.Named("Codex"), SessionActivity.Quiet, "승인이 필요합니다 · waiting for your input", selected = true),
        session("s4", "task-2", "Terminal 4", AgentRuntime.Named("Codex"), SessionActivity.Failed, "error: LoginTest failed", exitCode = 1),
        session("s5", "task-3", "Terminal 5", AgentRuntime.Named("Claude"), SessionActivity.Exited, exitCode = 0),
    )

    private fun session(
        id: String, task: String, title: String, agent: AgentRuntime, activity: SessionActivity,
        output: String? = null, selected: Boolean = false, exitCode: Int? = null,
    ) = AgentSessionRow(
        sessionId = id, title = title, agent = agent, activity = activity,
        lastOutputAtMillis = null, nowMillis = 0, exitCode = exitCode, previewUrl = null,
        selected = selected, lastLine = output, taskKey = task,
    )
}
