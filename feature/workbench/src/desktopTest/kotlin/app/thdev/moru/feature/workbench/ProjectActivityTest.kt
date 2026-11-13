package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProjectActivityTest {
    @Test
    fun projectSummaryRemainsPartOfTheProjectSelectionTarget() = runDesktopComposeUiTest {
        val selected = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(sidebar(), {}, { selected += it }, {}, { _, _ -> }, {}, Modifier.width(240.dp),
                    sessionSlots = NoSessionSlots, sessions = listOf(session("running", "alpha-task")), showHeader = false)
            }
        }
        onNodeWithTag("project-session-summary:alpha", useUnmergedTree = true).performTouchInput { click() }
        runOnIdle { assertEquals(listOf("alpha"), selected) }
    }

    @Test
    fun aPersistedApprovalDecisionStaysVisibleWhenItsTerminalIsIdle() = runDesktopComposeUiTest {
        val initial = sidebar()
        val project = initial.projects.first()
        val pending = project.copy(tasks = project.tasks.map {
            it.copy(displayState = TaskDisplayState.NeedsApproval, group = TaskGroup.NeedsAttention)
        })
        setContent {
            MoruTheme {
                ProjectTaskSidebar(initial.copy(projects = listOf(pending)), {}, {}, {}, { _, _ -> }, {},
                    Modifier.width(240.dp), sessionSlots = NoSessionSlots,
                    sessions = listOf(session("idle", "alpha-task").copy(activity = SessionActivity.Idle)),
                    showHeader = false)
            }
        }
        onNodeWithContentDescription("1 tasks need you in Alpha").assertExists()
    }

    @Test
    fun projectSummariesKeepMixedStatesSeparateAndUpdateAfterAResponseIsViewed() = runDesktopComposeUiTest {
        var rows by mutableStateOf(listOf(
            session("alpha-running", "alpha-task"),
            session("alpha-done", "alpha-task").copy(activity = SessionActivity.Quiet),
            session("beta-done", "beta-task").copy(activity = SessionActivity.Quiet),
        ))
        setContent {
            MoruTheme {
                ProjectTaskSidebar(sidebar(), {}, {}, {}, { _, _ -> }, {}, Modifier.width(240.dp),
                    sessionSlots = NoSessionSlots, sessions = rows, showHeader = false)
            }
        }
        fun summary(project: String, text: String) = onNode(
            hasText(text) and hasAnyAncestor(hasTestTag("project-session-summary:$project")),
            useUnmergedTree = true,
        )
        summary("alpha", "Running 1").assertExists()
        summary("alpha", "Response ready 1").assertExists()
        summary("beta", "Response ready 1").assertExists()
        summary("beta", "Running 1").assertDoesNotExist()
        runOnIdle { rows = rows.map { if (it.sessionId == "alpha-done") it.copy(activity = SessionActivity.Waiting) else it } }
        summary("alpha", "Response ready 1").assertDoesNotExist()
        summary("alpha", "Waiting 1").assertExists()
        summary("alpha", "Running 1").assertExists()
        summary("beta", "Response ready 1").assertExists()
    }

    @Test
    fun collapsedProjectsReceiveOnlyTheirOwnObservedTaskSessions() = runDesktopComposeUiTest {
        val rows = listOf(session("alpha-session", "alpha-task"), session("beta-session", "beta-task"), session("loose", null))
        val seen = mutableMapOf<String, List<String>>()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    sidebar(), {}, {}, {}, { _, _ -> }, {}, Modifier.width(260.dp),
                    sessionSlots = NoSessionSlots.copy(projectActivity = { spec, modifier ->
                        val owned = spec.rows
                        val ids = owned.map { it.sessionId }
                        MoruText(ids.joinToString(), modifier)
                        owned.firstOrNull()?.taskKey?.let { seen[it] = ids }
                    }),
                    sessions = rows, showHeader = false,
                )
            }
        }
        onNodeWithText("Alpha").assertExists()
        onNodeWithText("alpha-session").assertExists()
        onNodeWithText("beta-session").assertExists()
        onNodeWithText("loose").assertDoesNotExist()
        runOnIdle {
            assertEquals(mapOf("alpha-task" to listOf("alpha-session"), "beta-task" to listOf("beta-session")), seen)
        }
    }

    @Test
    fun expandingAProjectReplacesItsActivityLinesWithItsTaskRows() = runDesktopComposeUiTest {
        var state by mutableStateOf(sidebar().copy(projects = sidebar().projects.take(1)))
        val selected = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    state, {}, { selected += it }, {}, { _, _ -> }, {}, Modifier.width(240.dp),
                    sessionSlots = NoSessionSlots.copy(projectActivity = { _, modifier -> MoruText("activity strip", modifier) }),
                    sessions = listOf(session("alpha-session", "alpha-task")), showHeader = false,
                )
            }
        }
        onNodeWithText("activity strip").assertExists()
        onNodeWithText("Fix login").assertDoesNotExist()
        onNodeWithText("Alpha").performClick()
        runOnIdle {
            assertEquals(listOf("alpha"), selected)
            state = state.copy(projects = state.projects.map { it.copy(expanded = true) })
        }
        onNodeWithText("activity strip").assertDoesNotExist()
        onNodeWithText("Fix login").assertExists()
    }

    private fun sidebar(): ProjectTaskSidebarState = ProjectTaskSidebarState(
        projects = listOf("alpha" to "Alpha", "beta" to "Beta").map { (id, name) ->
            val task = TaskRow("$id-task", id, name, "Fix login", TaskDisplayState.Working, TaskGroup.Running,
                TaskAttention.None, true, false)
            ProjectRow(id, name, "/projects/$id", null, true, false, id == "alpha", listOf(task))
        },
        selectedProjectId = "alpha", selectedTaskId = null,
    )

    private fun session(id: String, task: String?) = AgentSessionRow(
        sessionId = id, title = "Reviewing changes", agent = AgentRuntime.Named("Codex"),
        activity = SessionActivity.Working, lastOutputAtMillis = null, nowMillis = 0,
        exitCode = null, previewUrl = null, selected = false, taskKey = task,
    )
}
