package app.thdev.moru.app

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.screenshot.TaskGroupingFixtures
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.ProjectTaskSidebar
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TaskSessionGroupInteractionTest {
    @Test
    fun duplicateTitlesAndNamedChildSessionsStillNavigateToTheirOwnTaskAndProject() = runDesktopComposeUiTest {
        val tasks = mutableListOf<String>()
        val sessions = mutableListOf<Pair<String, String?>>()
        val state = TaskGroupingFixtures.state()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    state, {}, {}, {}, { _, task -> tasks += task }, {}, Modifier.width(300.dp),
                    sessions = TaskGroupingFixtures.sessions(),
                    onSelectSession = { row, project -> sessions += row.sessionId to project },
                    sessionSlots = DesktopWorkbenchSessionSlots,
                )
            }
        }
        onAllNodesWithContentDescription("Task Terminal session")[1].assertIsSelected().performClick()
        onNodeWithText("Terminal 4").performClick()
        runOnIdle {
            assertEquals(listOf("task-2"), tasks)
            assertEquals(listOf("s4" to state.selectedProjectId), sessions)
        }
    }

    @Test
    fun theSameNamedSessionIsReachableFromTheAllSessionsPopover() = runDesktopComposeUiTest {
        val selected = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    TaskGroupingFixtures.state(), {}, {}, {}, { _, _ -> }, {}, Modifier.width(300.dp),
                    sessions = TaskGroupingFixtures.sessions(), onSelectSession = { row, _ -> selected += row.sessionId },
                    sessionSlots = DesktopWorkbenchSessionSlots,
                )
            }
        }
        onNodeWithText("Sessions").performClick()
        // Both the sidebar and popup contain the title; select the contextual popup row.
        onNodeWithContentDescription("Codex, Terminal 4, KeyFlow · Terminal session, Failed").performClick()
        runOnIdle { assertEquals(listOf("s4"), selected) }
    }
}
