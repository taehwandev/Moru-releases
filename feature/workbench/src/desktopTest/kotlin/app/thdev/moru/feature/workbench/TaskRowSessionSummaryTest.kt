package app.thdev.moru.feature.workbench

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.ui.identity.taskIdentityAccent
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.terminal.SessionPermissionPrompt
import app.thdev.moru.feature.workbench.screenshot.AttentionFixtures
import app.thdev.moru.feature.workbench.sidebar.TaskSessionGroup
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TaskRowSessionSummaryTest {
    @Test
    fun anEndedOnlySessionHasAnExplicitStateRatherThanAnUnexplainedTotal() = runDesktopComposeUiTest {
        val task = AttentionFixtures.sidebarState().projects.first().tasks.first()
        setContent {
            MoruTheme {
                TaskRowView(task, {}, accent = taskIdentityAccent(task.id), sessions = listOf(
                    AgentSessionRow("ended", "Shell", AgentRuntime.Terminal, SessionActivity.Exited,
                        null, 0, 0, null, false, taskKey = task.id)))
            }
        }
        onNodeWithText("1 session").assertExists()
        onNodeWithText("Ended 1").assertExists()
        onNodeWithText("Waiting 1").assertDoesNotExist()
    }

    @Test
    fun aLivePermissionKeepsAnIdleSessionVisibleAndItsSummaryNeedsAttention() {
        val task = AttentionFixtures.sidebarState().projects.first().tasks.first().copy(selected = false)
        val row = AgentSessionRow("idle-permission", "Awaiting permission", AgentRuntime.Named("Codex"),
            SessionActivity.Idle, null, 0, null, null, false, taskKey = task.id)
        SessionPermissions.observe(row.sessionId, SessionPermissionPrompt("Allow command?", "Command", emptyList()))
        try {
            runDesktopComposeUiTest {
                setContent {
                    MoruTheme {
                        TaskSessionGroup(task, listOf(row), {}, {}, TaskRowActions(), { _, _ -> },
                            sessionRow = { spec, modifier -> Text(spec.row.title, modifier) },
                            revealSelection = false, collapseIdleSessions = true)
                    }
                }
                onNodeWithText("Awaiting permission").assertExists()
                onNodeWithText("Needs attention 1").assertExists()
                onNodeWithText("Waiting 1").assertDoesNotExist()
            }
        } finally {
            SessionPermissions.forget(row.sessionId)
        }
    }

    @Test
    fun sessionChangesKeepRunningAndFinishedResponsesVisibleTogether() = runDesktopComposeUiTest {
        val task = AttentionFixtures.sidebarState().projects.first().tasks.first()
        val rows = mutableStateOf(listOf(SessionActivity.Working, SessionActivity.Asking, SessionActivity.Failed, SessionActivity.Quiet)
            .mapIndexed { index, activity ->
                AgentSessionRow("s$index", "Session", AgentRuntime.Named("Codex"), activity,
                    null, 0, null, null, false, taskKey = task.id)
            })
        setContent {
            MoruTheme {
                TaskRowView(task, {}, accent = taskIdentityAccent(task.id), sessions = rows.value)
            }
        }
        onNodeWithText("4 sessions").assertExists()
        onNodeWithText("Running 1").assertExists()
        onNodeWithText("Needs attention 2").assertExists()
        onNodeWithText("Response ready 1").assertExists()
        runOnIdle { rows.value = rows.value.takeLast(1) }
        onNodeWithText("1 session").assertExists()
        onNodeWithText("Response ready 1").assertExists()
        onNodeWithText("Running 1").assertDoesNotExist()
        runOnIdle { rows.value = emptyList() }
        onNodeWithText("0 sessions").assertExists()
    }

    @Test
    fun collapseIdleSessionsKeepsFinishedResponsesAndQuietRunningPrograms() = runDesktopComposeUiTest {
        val task = AttentionFixtures.sidebarState().projects.first().tasks.first().copy(selected = false)
        val sessions = listOf(
            AgentSessionRow("s-work", "Active Build", AgentRuntime.Terminal, SessionActivity.Working, null, 0, null, null, false, taskKey = task.id),
            AgentSessionRow("s-ask", "Needs Approval", AgentRuntime.Terminal, SessionActivity.Asking, null, 0, null, null, false, taskKey = task.id),
            AgentSessionRow("s-idle", "Idle Shell", AgentRuntime.Terminal, SessionActivity.Idle, null, 0, null, null, false, taskKey = task.id),
            AgentSessionRow("s-quiet", "Quiet Shell", AgentRuntime.Terminal, SessionActivity.Quiet, null, 0, null, null, false, taskKey = task.id),
            AgentSessionRow("s-done", "Updated links", AgentRuntime.Named("Claude"), SessionActivity.Quiet, null, 0, null, null, false, taskKey = task.id),
        )
        setContent {
            MoruTheme {
                TaskSessionGroup(
                    task = task,
                    sessions = sessions,
                    onSelectTask = {},
                    onSelectSession = {},
                    taskActions = TaskRowActions(),
                    onOpenPreview = { _, _ -> },
                    sessionRow = { spec, modifier -> Text(spec.row.title, modifier = modifier) },
                    revealSelection = false,
                    collapseIdleSessions = true,
                )
            }
        }
        onNodeWithText("5 sessions").assertExists()
        onNodeWithText("Running 2").assertExists()
        onNodeWithText("Needs attention 1").assertExists()
        onNodeWithText("Response ready 1").assertExists()
        onNodeWithText("Active Build").assertExists()
        onNodeWithText("Needs Approval").assertExists()
        onAllNodesWithText("Idle Shell").assertCountEquals(0)
        onNodeWithText("Quiet Shell").assertExists()
        onNodeWithText("Updated links").assertExists()
    }
}
