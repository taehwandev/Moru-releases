package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.TaskAttention
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SidebarViewSwitchTest {
    @Test
    fun theTitleMenuSwitchesBetweenTheProjectTreeAndTheStatusBoard() = runDesktopComposeUiTest {
        var view by mutableStateOf(SidebarView.Projects)
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    sidebarState(), onOpenProject = {}, onSelectProject = {}, onToggleProject = {},
                    onSelectTask = { _, _ -> }, onNewTask = {}, modifier = Modifier.width(260.dp),
                    sessionSlots = NoSessionSlots.copy(board = { _, _ -> MoruText("board") }),
                    showHeader = false, view = view, onViewChange = { view = it },
                )
            }
        }
        onNodeWithText("Fix login").assertExists()
        onNodeWithText("board").assertDoesNotExist()

        onNodeWithContentDescription("Sidebar view: Projects").performClick()
        onNodeWithText("Status").performClick()
        runOnIdle { assertEquals(SidebarView.Board, view) }
        onNodeWithText("board").assertExists()
        onNodeWithText("Fix login").assertDoesNotExist()

        onNodeWithContentDescription("Sidebar view: Status").performClick()
        onNodeWithText("Projects").performClick()
        onNodeWithText("Fix login").assertExists()
    }

    @Test
    fun withoutAViewTheSidebarOffersNoSwitch() = runDesktopComposeUiTest {
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    sidebarState(), onOpenProject = {}, onSelectProject = {}, onToggleProject = {},
                    onSelectTask = { _, _ -> }, onNewTask = {}, modifier = Modifier.width(260.dp),
                    sessionSlots = NoSessionSlots, showHeader = false,
                )
            }
        }
        onNodeWithContentDescription("Sidebar view", substring = true).assertDoesNotExist()
        onNodeWithText("Fix login").assertExists()
    }

    private fun sidebarState(): ProjectTaskSidebarState {
        val task = TaskRow("task-1", "alpha", "Alpha", "Fix login", TaskDisplayState.Working, TaskGroup.Running, TaskAttention.None, true, false)
        return ProjectTaskSidebarState(
            listOf(ProjectRow("alpha", "Alpha", "/projects/alpha", null, true, true, true, listOf(task))), "alpha", null,
        )
    }
}
