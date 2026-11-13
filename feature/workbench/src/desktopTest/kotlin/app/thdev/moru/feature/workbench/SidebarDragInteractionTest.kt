package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Real desktop pointer and keyboard input exercise the sidebar's sibling-order callback contract. */
@OptIn(ExperimentalTestApi::class)
class SidebarDragInteractionTest {
    @Test
    fun taskDragOrdersEverySiblingAndKeepsItsTerminalInsideTheGroup() = runDesktopComposeUiTest {
        val taskOrders = mutableListOf<Pair<String, List<String>>>()
        val selections = mutableListOf<String>()
        showSidebar(taskOrders = taskOrders, selections = selections)
        val handle = "sidebar-drag:alpha:a1"
        val terminal = onNodeWithTag("terminal:session-a3").fetchSemanticsNode().boundsInRoot
        dragTo(handle, Offset(handleBounds(handle).center.x, terminal.bottom - 2f))

        runOnIdle {
            assertEquals(listOf("alpha" to listOf("a2", "a3", "a1")), taskOrders)
            assertEquals(emptyList(), selections)
        }
        val first = groupBounds("alpha", "a2")
        val last = groupBounds("alpha", "a1")
        assertTrue(first.top < last.top)
        val thirdGroup = groupBounds("alpha", "a3")
        val thirdTerminal = onNodeWithTag("terminal:session-a3").fetchSemanticsNode().boundsInRoot
        assertTrue(thirdTerminal.top >= thirdGroup.top && thirdTerminal.bottom <= thirdGroup.bottom)
        val capture = File("build/ui-captures/sidebar-drag-reordered.png")
        capture.parentFile.mkdirs()
        ImageIO.write(onNodeWithTag("sidebar-drag-fixture").captureToImage().toAwtImage(), "png", capture)
    }

    @Test
    fun projectDragMovesTheEntireExpandedSectionAndCanInsertBeforeTheFirst() = runDesktopComposeUiTest {
        val orders = mutableListOf<List<String>>()
        val selections = mutableListOf<String>()
        showSidebar(projectOrders = orders, selections = selections)
        val handle = "sidebar-drag:projects:beta"
        val alpha = groupBounds("projects", "alpha")
        dragTo(handle, Offset(handleBounds(handle).center.x, alpha.top + 2f))

        runOnIdle {
            assertEquals(listOf(listOf("beta", "alpha", "gamma")), orders)
            assertEquals(emptyList(), selections)
        }
        assertTrue(groupBounds("projects", "beta").top < groupBounds("projects", "alpha").top)
        assertTrue(groupBounds("beta", "b1").top < groupBounds("projects", "alpha").top)
    }

    @Test
    fun droppingOutsideTheSidebarOrInsideAnotherProjectDoesNothing() = runDesktopComposeUiTest {
        val projectOrders = mutableListOf<List<String>>()
        val taskOrders = mutableListOf<Pair<String, List<String>>>()
        showSidebar(projectOrders, taskOrders)
        val taskHandle = "sidebar-drag:alpha:a1"
        val otherProject = groupBounds("beta", "b2")
        dragTo(taskHandle, Offset(handleBounds(taskHandle).center.x, otherProject.center.y))
        dragTo(taskHandle, Offset(430f, groupBounds("alpha", "a3").bottom - 2f))
        dragTo("sidebar-drag:projects:alpha", Offset(430f, groupBounds("projects", "beta").center.y))

        runOnIdle {
            assertEquals(emptyList(), taskOrders)
            assertEquals(emptyList(), projectOrders)
        }
    }

    @Test
    fun canceledPointerAndEscapeDuringDragEmitNoOrder() = runDesktopComposeUiTest {
        val projectOrders = mutableListOf<List<String>>()
        val taskOrders = mutableListOf<Pair<String, List<String>>>()
        showSidebar(projectOrders, taskOrders)
        val taskHandle = "sidebar-drag:alpha:a1"
        val destination = groupBounds("alpha", "a3")
        val origin = handleBounds(taskHandle).topLeft
        onNodeWithTag(taskHandle).performMouseInput {
            moveTo(center)
            press()
            moveTo(Offset(center.x, destination.bottom - 2f - origin.y), delayMillis = 100)
            cancel()
        }

        val projectHandle = "sidebar-drag:projects:alpha"
        val projectOrigin = handleBounds(projectHandle).topLeft
        val beta = groupBounds("projects", "beta")
        onNodeWithTag(projectHandle).performClick()
        onNodeWithTag(projectHandle).performMouseInput {
            moveTo(center)
            press()
            moveTo(Offset(center.x, beta.bottom - 2f - projectOrigin.y), delayMillis = 100)
        }
        onNodeWithTag(projectHandle).performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag(projectHandle).performMouseInput { release() }

        runOnIdle {
            assertEquals(emptyList(), taskOrders)
            assertEquals(emptyList(), projectOrders)
        }
    }

    @Test
    fun handlesOfferKeyboardMovesWhileNormalClicksStillSelectRows() = runDesktopComposeUiTest {
        val taskOrders = mutableListOf<Pair<String, List<String>>>()
        val projectOrders = mutableListOf<List<String>>()
        val selections = mutableListOf<String>()
        showSidebar(projectOrders, taskOrders, selections)
        onNodeWithContentDescription("Reorder task First", substring = true).performClick()
        onNodeWithTag("sidebar-drag:alpha:a1").performKeyInput { withKeyDown(Key.AltLeft) { pressKey(Key.DirectionDown) } }
        onNodeWithTag("sidebar-drag:projects:beta").performClick()
        onNodeWithTag("sidebar-drag:projects:beta").performKeyInput { withKeyDown(Key.AltLeft) { pressKey(Key.DirectionUp) } }
        runOnIdle {
            assertEquals(listOf("alpha" to listOf("a2", "a1", "a3")), taskOrders)
            assertEquals(listOf(listOf("beta", "alpha", "gamma")), projectOrders)
            assertEquals(emptyList(), selections)
        }
        onNodeWithContentDescription("Task First").performClick()
        onNodeWithContentDescription("Project Beta").performClick()
        runOnIdle { assertEquals(listOf("task:alpha:a1", "project:beta"), selections) }
    }

    @Test
    fun handlesStayHiddenUntilReorderModeIsToggledAndAbsentCallbacksHideTheToggle() = runDesktopComposeUiTest {
        showSidebar(enableReorder = false, enterReorderMode = false)
        onNodeWithTag("sidebar-reorder-toggle").assertDoesNotExist()
        onNodeWithTag("sidebar-drag:alpha:a1").assertDoesNotExist()
        onNodeWithTag("sidebar-drag:projects:alpha").assertDoesNotExist()
    }

    @Test
    fun headerReorderToggleShowsAndHidesHandles() = runDesktopComposeUiTest {
        showSidebar(enableReorder = true, enterReorderMode = false)
        onNodeWithTag("sidebar-drag:alpha:a1").assertDoesNotExist()
        onNodeWithTag("sidebar-drag:projects:alpha").assertDoesNotExist()
        onNodeWithContentDescription("Actions for Alpha").assertExists()
        onNodeWithContentDescription("Actions for First").assertExists()

        onNodeWithTag("sidebar-reorder-toggle").performClick()
        onNodeWithTag("sidebar-drag:alpha:a1").assertExists()
        onNodeWithTag("sidebar-drag:projects:alpha").assertExists()
        onNodeWithContentDescription("Actions for Alpha").assertDoesNotExist()
        onNodeWithContentDescription("Actions for First").assertDoesNotExist()

        onNodeWithTag("sidebar-reorder-toggle").performClick()
        onNodeWithTag("sidebar-drag:alpha:a1").assertDoesNotExist()
        onNodeWithTag("sidebar-drag:projects:alpha").assertDoesNotExist()
        onNodeWithContentDescription("Actions for Alpha").assertExists()
        onNodeWithContentDescription("Actions for First").assertExists()
    }

    @Test
    fun viewSwitcherShowsReorderToggleWhenSidebarHeaderIsHidden() = runDesktopComposeUiTest {
        showSidebar(enableReorder = true, enterReorderMode = false, showHeader = false, view = SidebarView.Projects)
        onNodeWithTag("sidebar-drag:alpha:a1").assertDoesNotExist()
        onNodeWithTag("sidebar-drag:projects:alpha").assertDoesNotExist()

        onNodeWithTag("sidebar-reorder-toggle").performClick()
        onNodeWithTag("sidebar-drag:alpha:a1").assertExists()
        onNodeWithTag("sidebar-drag:projects:alpha").assertExists()

        onNodeWithTag("sidebar-reorder-toggle").performClick()
        onNodeWithTag("sidebar-drag:alpha:a1").assertDoesNotExist()
        onNodeWithTag("sidebar-drag:projects:alpha").assertDoesNotExist()
    }

    private fun ComposeUiTest.showSidebar(
        projectOrders: MutableList<List<String>> = mutableListOf(),
        taskOrders: MutableList<Pair<String, List<String>>> = mutableListOf(),
        selections: MutableList<String> = mutableListOf(),
        enableReorder: Boolean = true,
        enterReorderMode: Boolean = enableReorder,
        showHeader: Boolean = true,
        view: SidebarView? = null,
    ) {
        setContent {
            var state by remember { mutableStateOf(dragState()) }
            MoruTheme {
                ProjectTaskSidebar(
                    state = state,
                    onOpenProject = {},
                    onSelectProject = { selections += "project:$it" },
                    onToggleProject = { id -> state = state.copy(projects = state.projects.map { if (it.id == id) it.copy(expanded = !it.expanded) else it }) },
                    onSelectTask = { project, task -> selections += "task:$project:$task" },
                    onNewTask = {},
                    modifier = Modifier.width(360.dp).height(640.dp).testTag("sidebar-drag-fixture"),
                    sessionSlots = NoSessionSlots.copy(row = { spec, modifier ->
                        Box(modifier.fillMaxWidth().height(42.dp).testTag("terminal:${spec.row.sessionId}")) {
                            MoruText("Terminal ${spec.row.sessionId}")
                        }
                    }),
                    sessions = listOf(session("session-a3", "a3"), session("session-b2", "b2")),
                    onReorderProjects = if (enableReorder) {
                        { ids ->
                            projectOrders += ids
                            state = state.copy(projects = ids.map { id -> state.projects.first { it.id == id } })
                        }
                    } else null,
                    onReorderTasks = if (enableReorder) {
                        { projectId, ids ->
                            taskOrders += projectId to ids
                            state = state.copy(projects = state.projects.map { project ->
                                if (project.id == projectId) project.copy(tasks = ids.map { id -> project.tasks.first { it.id == id } }) else project
                            })
                        }
                    } else null,
                    showHeader = showHeader,
                    view = view,
                )
            }
        }
        if (enterReorderMode) {
            onNodeWithTag("sidebar-reorder-toggle").performClick()
        }
    }

    private fun ComposeUiTest.dragTo(handle: String, destination: Offset) {
        val origin = handleBounds(handle).topLeft
        onNodeWithTag(handle).performMouseInput {
            moveTo(center)
            press()
            moveTo(destination - origin, delayMillis = 100)
            release()
        }
    }

    private fun ComposeUiTest.handleBounds(tag: String) = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
    private fun ComposeUiTest.groupBounds(scope: String, id: String) = onNodeWithTag("sidebar-group:$scope:$id").fetchSemanticsNode().boundsInRoot

    private fun dragState(): ProjectTaskSidebarState {
        fun task(id: String, project: String, name: String, title: String) =
            TaskRow(id, project, name, title, TaskDisplayState.Working, TaskGroup.Running, TaskAttention.None, true, false)
        return ProjectTaskSidebarState(
            projects = listOf(
                ProjectRow("alpha", "Alpha", "/alpha", null, true, true, true,
                    listOf(task("a1", "alpha", "Alpha", "First"), task("a2", "alpha", "Alpha", "Second"), task("a3", "alpha", "Alpha", "Third"))),
                ProjectRow("beta", "Beta", "/beta", null, true, true, false,
                    listOf(task("b1", "beta", "Beta", "Beta first"), task("b2", "beta", "Beta", "Beta second"))),
                ProjectRow("gamma", "Gamma", "/gamma", null, true, false, false, emptyList()),
            ),
            selectedProjectId = "alpha",
            selectedTaskId = null,
        )
    }

    private fun session(id: String, task: String) = AgentSessionRow(
        sessionId = id, title = id, agent = AgentRuntime.Terminal, activity = SessionActivity.Idle,
        lastOutputAtMillis = null, nowMillis = 0, exitCode = null, previewUrl = null, selected = false, taskKey = task,
    )
}
