package app.thdev.moru.app

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.feature.workbench.ProjectRow
import app.thdev.moru.feature.workbench.ProjectTaskSidebar
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup
import app.thdev.moru.feature.workbench.TaskRow
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Background activity may change the content above the user's reading position without navigating. */
@OptIn(ExperimentalTestApi::class)
class SidebarActivityScrollStabilityTest {
    @Test
    fun upstreamActivityAndStreamingChangesKeepAManuallyScrolledCollapsedProjectInPlace() =
        runDesktopComposeUiTest(width = 240, height = 360) {
            val fixture = Fixture(projectCount = 12)
            showSidebar(fixture, height = 360)
            val anchor = onNodeWithTag("project-agent-line:session-6")
            val project = onNodeWithContentDescription("Project Project 6")
            anchor.performScrollTo()
            waitForIdle()
            val sessionY = screenY(anchor)
            val projectY = screenY(project)

            fun update(label: String, change: () -> Unit) {
                runOnIdle(change)
                waitForIdle()
                assertStaysAt(anchor, sessionY, label)
                assertStaysAt(project, projectY, label)
                fixture.assertNoNavigation()
            }

            update("streamed output grows to two lines") {
                fixture.changeSession("session-0") { it.copy(lastLine = LongHeadline) }
            }
            update("streamed output shrinks to one line") {
                fixture.changeSession("session-0") { it.copy(lastLine = "Checking") }
            }
            update("a streaming summary takes over the headline") {
                fixture.changeSession("session-1") { it.copy(summary = LongHeadline) }
            }
            update("a completed turn falls back to a longer session title") {
                fixture.changeSession("session-0") {
                    it.copy(activity = SessionActivity.Quiet, title = LongHeadline, lastLine = null)
                }
            }
            update("an idle session removes its preview and Task heading") {
                fixture.changeSession("session-0") { it.copy(activity = SessionActivity.Waiting) }
            }
            onNodeWithTag("project-agent-line:session-0").assertDoesNotExist()
            onNodeWithTag("project-agent-task:task-0").assertDoesNotExist()
            update("a new working session adds back its preview and Task heading") {
                fixture.changeSession("session-0") { it.copy(activity = SessionActivity.Working, lastLine = "Reading") }
            }
            onNodeWithTag("project-agent-line:session-0").assertExists()
            update("the upstream Task heading wraps") {
                fixture.changeTaskTitle("task-1", LongTaskTitle)
            }
            update("another completed preview disappears") {
                fixture.changeSession("session-1") { it.copy(activity = SessionActivity.Waiting) }
            }
            update("multiple upstream previews return together") {
                fixture.sessions = fixture.sessions.map {
                    if (it.sessionId in setOf("session-0", "session-1", "session-2")) {
                        it.copy(activity = SessionActivity.Working, summary = null, title = "Session", lastLine = "Done reading")
                    } else it
                }
            }
        }

    @Test
    fun removingRowsAndTaskHeadingsInsideATallCollapsedProjectKeepsItsSurvivingSessionInPlace() =
        runDesktopComposeUiTest(width = 240, height = 320) {
            val fixture = Fixture(projectCount = 6, firstProjectTasks = 4)
            fixture.state = fixture.state.copy(projects = fixture.state.projects.map { project ->
                if (project.id == "project-0") {
                    project.copy(tasks = project.tasks.map { it.copy(title = LongTaskTitle) })
                } else project
            })
            showSidebar(fixture, height = 320)
            val anchor = onNodeWithTag("project-agent-line:session-0-3")
            anchor.performScrollTo()
            // Move the first preview fully above the viewport while leaving the fourth one visible.
            onNodeWithTag("sidebar-viewport").performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                assertTrue(scrollBy(0f, with(density) { 180.dp.toPx() }))
            }
            waitForIdle()
            val viewport = onNodeWithTag("sidebar-viewport").getUnclippedBoundsInRoot()
            assertTrue(onNodeWithTag("project-agent-line:session-0").getUnclippedBoundsInRoot().bottom <= viewport.top,
                "The changed preview must be above the viewport to exercise in-project anchoring")
            val anchorY = screenY(anchor)

            fun update(label: String, change: () -> Unit) {
                runOnIdle(change)
                waitForIdle()
                assertStaysAt(anchor, anchorY, label)
                fixture.assertNoNavigation()
            }

            update("remove a preview and its Task heading above the viewport") {
                fixture.changeSession("session-0") { it.copy(activity = SessionActivity.Waiting) }
            }
            onNodeWithTag("project-agent-task:task-0").assertDoesNotExist()
            update("remove the next Task group while the later session survives") {
                fixture.changeSession("session-0-1") { it.copy(activity = SessionActivity.Waiting) }
            }
            update("restore a preceding group with a streaming headline") {
                fixture.changeSession("session-0") { it.copy(activity = SessionActivity.Working, lastLine = LongHeadline) }
            }
            update("a preceding working session completes") {
                fixture.changeSession("session-0-2") { it.copy(activity = SessionActivity.Quiet, title = LongHeadline) }
            }
            update("a preceding summary becomes shorter") {
                fixture.changeSession("session-0") { it.copy(summary = "Ready") }
            }
        }

    @Test
    fun backgroundActivityGrowthLeavesTheSidebarAtTheTop() = runDesktopComposeUiTest(width = 240, height = 320) {
        val fixture = Fixture(projectCount = 8, firstProjectTasks = 4)
        fixture.sessions = fixture.sessions.map {
            if (it.taskKey?.startsWith("task-0") == true) it.copy(activity = SessionActivity.Waiting) else it
        }
        showSidebar(fixture, height = 320)
        val firstProject = onNodeWithContentDescription("Project Project 0")
        val topY = screenY(firstProject)
        runOnIdle {
            fixture.sessions = fixture.sessions.map {
                if (it.taskKey?.startsWith("task-0") == true) {
                    it.copy(activity = SessionActivity.Working, lastLine = LongHeadline)
                } else it
            }
        }
        waitForIdle()
        assertStaysAt(firstProject, topY, "new activity while already at the top")
        runOnIdle { fixture.changeTaskTitle("task-0", LongTaskTitle) }
        waitForIdle()
        assertStaysAt(firstProject, topY, "a growing Task heading while already at the top")
        fixture.assertNoNavigation()
    }

    private fun ComposeUiTest.showSidebar(fixture: Fixture, height: Int) {
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    state = fixture.state,
                    onOpenProject = { fixture.navigationCalls += "open" },
                    onSelectProject = { fixture.navigationCalls += "project:$it" },
                    onToggleProject = { fixture.navigationCalls += "toggle:$it" },
                    onSelectTask = { project, task -> fixture.navigationCalls += "task:$project:$task" },
                    onNewTask = { fixture.navigationCalls += "new:$it" },
                    modifier = Modifier.width(240.dp).height(height.dp),
                    sessionSlots = DesktopWorkbenchSessionSlots,
                    sessions = fixture.sessions,
                    onSelectSession = { row, _ -> fixture.navigationCalls += "session:${row.sessionId}" },
                    showHeader = false,
                )
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.screenY(node: SemanticsNodeInteraction): Float {
        node.assertIsDisplayed()
        val bounds = node.getUnclippedBoundsInRoot()
        val viewport = onNodeWithTag("sidebar-viewport").getUnclippedBoundsInRoot()
        val tolerance = with(density) { 1.toDp() }
        assertTrue(bounds.top >= viewport.top - tolerance && bounds.bottom <= viewport.bottom + tolerance,
            "Reading anchor $bounds must remain fully visible inside $viewport")
        return with(density) { bounds.top.toPx() }
    }

    private fun ComposeUiTest.assertStaysAt(node: SemanticsNodeInteraction, beforeY: Float, reason: String) {
        val afterY = screenY(node)
        assertTrue(abs(afterY - beforeY) <= 1f,
            "$reason moved the reading anchor from $beforeY px to $afterY px")
    }

    private class Fixture(projectCount: Int, firstProjectTasks: Int = 1) {
        var state by mutableStateOf(ProjectTaskSidebarState(
            projects = List(projectCount) { index ->
                val projectId = "project-$index"
                val projectName = "Project $index"
                ProjectRow(projectId, projectName, "/projects/$index", null, true, false, index == 0,
                    List(if (index == 0) firstProjectTasks else 1) { taskIndex ->
                        val taskId = "task-$index" + if (taskIndex == 0) "" else "-$taskIndex"
                        TaskRow(taskId, projectId, projectName, "Task $index-$taskIndex", TaskDisplayState.Working,
                            TaskGroup.Running, TaskAttention.None, true, false)
                    })
            },
            selectedProjectId = "project-0",
            selectedTaskId = null,
        ))
        var sessions by mutableStateOf(state.projects.flatMap { project -> project.tasks.map { task ->
            AgentSessionRow(
                sessionId = task.id.replace("task-", "session-"),
                title = "Session",
                agent = AgentRuntime.Named("Codex"),
                activity = SessionActivity.Working,
                lastOutputAtMillis = null,
                nowMillis = 0,
                exitCode = null,
                previewUrl = null,
                selected = false,
                taskKey = task.id,
            )
        } })
        val navigationCalls = mutableListOf<String>()

        fun changeSession(id: String, update: (AgentSessionRow) -> AgentSessionRow) {
            sessions = sessions.map { if (it.sessionId == id) update(it) else it }
        }

        fun changeTaskTitle(id: String, title: String) {
            state = state.copy(projects = state.projects.map { project ->
                project.copy(tasks = project.tasks.map { if (it.id == id) it.copy(title = title) else it })
            })
        }

        fun assertNoNavigation() {
            assertEquals(emptyList(), navigationCalls, "Background updates must not request navigation")
            assertEquals("project-0", state.selectedProjectId)
            assertEquals(null, state.selectedTaskId)
            assertTrue(sessions.none { it.selected }, "Activity updates must not manufacture a session selection")
        }
    }

    private companion object {
        const val LongHeadline = "Reading the latest streaming output and checking the sidebar presentation across project boundaries"
        const val LongTaskTitle = "Inspect background activity and preserve the current reading position"
    }
}
