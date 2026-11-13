package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.launch.NewTaskComposer
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies the entry controls and callbacks, rather than treating screenshots as runtime proof. */
@OptIn(ExperimentalTestApi::class)
class WorkbenchEntryInteractionTest {
    @Test
    fun emptyCatalogOffersOpenProjectAndDisablesNewTask() = runDesktopComposeUiTest {
        var opens = 0
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    ProjectTaskSidebarState(emptyList(), null, null),
                    onOpenProject = { opens++ }, onSelectProject = {}, onToggleProject = {},
                    onSelectTask = { _, _ -> }, onNewTask = {}, modifier = Modifier.width(260.dp),
                    sessionSlots = NoSessionSlots,
                )
            }
        }
        onNodeWithContentDescription("New task in", substring = true).assertDoesNotExist()
        onNodeWithContentDescription("Open project…").performClick()
        runOnIdle { assertEquals(1, opens) }
    }

    private fun sidebarWithRenames(renames: MutableList<Pair<String, String>>): @androidx.compose.runtime.Composable () -> Unit = {
        MoruTheme {
            ProjectTaskSidebar(
                entryState(), onOpenProject = {}, onSelectProject = {}, onToggleProject = {}, onSelectTask = { _, _ -> },
                onNewTask = {}, modifier = Modifier.width(260.dp), onRenameProject = { id, name -> renames += id to name },
                sessionSlots = NoSessionSlots,
            )
        }
    }

    @Test
    fun projectCanBeRenamedAndSaveHandsOverTheTrimmedName() = runDesktopComposeUiTest {
        val renames = mutableListOf<Pair<String, String>>()
        setContent(sidebarWithRenames(renames))

        onNodeWithContentDescription("Actions for Alpha").performClick()
        onNodeWithText("Rename").performClick()
        onNodeWithTag("project-name").performTextClearance()
        onNodeWithTag("project-name").performTextInput("  Alpha app ")
        onNodeWithContentDescription("Save name").performClick()

        runOnIdle { assertEquals(listOf("alpha" to "Alpha app"), renames) }
        onNodeWithTag("project-name").assertDoesNotExist()
    }

    @Test
    fun theProjectMenuAsksWhereTheFolderIsNow() = runDesktopComposeUiTest {
        val relocated = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    entryState(), onOpenProject = {}, onSelectProject = {}, onToggleProject = {}, onSelectTask = { _, _ -> },
                    onNewTask = {}, modifier = Modifier.width(260.dp), onRelocateProject = { id -> relocated += id },
                    sessionSlots = NoSessionSlots,
                )
            }
        }

        onNodeWithContentDescription("Actions for Alpha").performClick()
        onNodeWithText("Change folder…").performClick()

        runOnIdle { assertEquals(listOf("alpha"), relocated) }
        onNodeWithText("Change folder…").assertDoesNotExist()
    }

    @Test
    fun escapeAndTheCrossLeaveTheNameAsItWasAndABlankOrUnchangedNameIsNotSaved() = runDesktopComposeUiTest {
        val renames = mutableListOf<Pair<String, String>>()
        setContent(sidebarWithRenames(renames))

        onNodeWithContentDescription("Actions for Alpha").performClick()
        onNodeWithText("Rename").performClick()
        onNodeWithTag("project-name").performTextClearance()
        onNodeWithContentDescription("Save name").performClick()
        onNodeWithTag("project-name").assertExists()
        // Pressing the save mark moved the focus, so put it back in the field before pressing Escape.
        onNodeWithTag("project-name").performClick()
        onNodeWithTag("project-name").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("project-name").assertDoesNotExist()

        onNodeWithContentDescription("Actions for Alpha").performClick()
        onNodeWithText("Rename").performClick()
        onNodeWithContentDescription("Save name").performClick()
        onNodeWithContentDescription("Cancel renaming").performClick()
        onNodeWithTag("project-name").assertDoesNotExist()

        runOnIdle { assertEquals(emptyList(), renames) }
    }

    private fun sidebarWithTaskActions(calls: MutableList<String>): @androidx.compose.runtime.Composable () -> Unit = {
        var state by remember { mutableStateOf(entryState()) }
        MoruTheme {
            ProjectTaskSidebar(
                state, onOpenProject = {}, onSelectProject = {}, onToggleProject = {},
                onSelectTask = { project, task -> state = state.select(project, task) },
                onNewTask = {}, modifier = Modifier.width(260.dp),
                taskActions = TaskRowActions(
                    onRename = { id, title -> calls += "rename:$id:$title" }, onDelete = { calls += "delete:$it" },
                ),
                sessionSlots = NoSessionSlots,
            )
        }
    }

    @Test
    fun taskMenuOffersRenameAndDelete() = runDesktopComposeUiTest {
        val calls = mutableListOf<String>()
        setContent(sidebarWithTaskActions(calls))

        onNodeWithContentDescription("Actions for Fix login").performClick()
        onNodeWithText("Rename").assertExists()
        onNodeWithText("Delete").assertExists()
        onNodeWithText("Mark done").assertDoesNotExist()
    }

    @Test
    fun aTaskIsRenamedInPlaceAndDeletedOnlyAfterTheConfirmation() = runDesktopComposeUiTest {
        val calls = mutableListOf<String>()
        setContent(sidebarWithTaskActions(calls))
        onNodeWithContentDescription("Task Fix login").performClick()

        onNodeWithContentDescription("Actions for Fix login").performClick()
        onNodeWithText("Rename").performClick()
        onNodeWithTag("task-name").performTextClearance()
        onNodeWithTag("task-name").performTextInput("Fix signup")
        onNodeWithContentDescription("Save name").performClick()

        onNodeWithContentDescription("Actions for Fix login").performClick()
        onNodeWithText("Delete").performClick()
        onNodeWithText("Keep").performClick()
        onNodeWithContentDescription("Actions for Fix login").performClick()
        onNodeWithText("Delete").performClick()
        onNodeWithText("Delete task").performClick()

        runOnIdle { assertEquals(listOf("rename:task-1:Fix signup", "delete:task-1"), calls) }
    }

    @Test
    fun closedProjectsAreFoldedAwayAndOnePressReopensByItsFolder() = runDesktopComposeUiTest {
        val reopened = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    entryState().copy(closedProjects = listOf(ClosedProjectRow("g", "Gamma", "/projects/gamma"), ClosedProjectRow("d", "Delta", "/projects/delta"))),
                    onOpenProject = {}, onSelectProject = {}, onToggleProject = {}, onSelectTask = { _, _ -> },
                    onNewTask = {}, modifier = Modifier.width(260.dp), onReopenProject = { reopened += it },
                    sessionSlots = NoSessionSlots,
                )
            }
        }

        onNodeWithText("Gamma").assertDoesNotExist()
        onNodeWithText("Closed projects · 2").performClick()
        onNodeWithContentDescription("Reopen Gamma").performClick()

        runOnIdle { assertEquals(listOf("/projects/gamma"), reopened) }
    }

    @Test
    fun aClosedProjectIsDeletedFromTheListOnlyAfterTheConfirmation() = runDesktopComposeUiTest {
        val forgotten = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    entryState().copy(closedProjects = listOf(ClosedProjectRow("g", "Gamma", "/projects/gamma"))),
                    onOpenProject = {}, onSelectProject = {}, onToggleProject = {}, onSelectTask = { _, _ -> },
                    onNewTask = {}, modifier = Modifier.width(260.dp), onForgetClosedProject = { forgotten += it },
                    sessionSlots = NoSessionSlots,
                )
            }
        }

        onNodeWithText("Closed projects · 1").performClick()
        onNodeWithContentDescription("Delete Gamma from the list").performClick()
        onNodeWithText("Its folder and files are not touched", substring = true).assertExists()
        onNodeWithText("Keep").performClick()
        onNodeWithText("Its folder and files are not touched", substring = true).assertDoesNotExist()
        runOnIdle { assertEquals(emptyList(), forgotten) }

        onNodeWithContentDescription("Delete Gamma from the list").performClick()
        onNodeWithText("Delete from list").performClick()

        runOnIdle { assertEquals(listOf("g"), forgotten) }
    }

    @Test
    fun withNothingClosedThereIsNoClosedSection() = runDesktopComposeUiTest {
        setContent { MoruTheme { ProjectTaskSidebar(entryState(), {}, {}, {}, { _, _ -> }, {}, Modifier.width(260.dp), sessionSlots = NoSessionSlots) } }

        onNodeWithText("Closed projects", substring = true).assertDoesNotExist()
    }

    @Test
    fun eachProjectCardsNewTaskRowStartsATaskInThatProject() = runDesktopComposeUiTest {
        val newTasks = mutableListOf<String>()
        val selectedTasks = mutableListOf<Pair<String, String>>()
        setContent {
            var state by remember { mutableStateOf(entryState()) }
            MoruTheme {
                ProjectTaskSidebar(
                    state, onOpenProject = {},
                    onSelectProject = { id -> state = state.select(id, null) },
                    onToggleProject = { id ->
                        state = state.copy(projects = state.projects.map { if (it.id == id) it.copy(expanded = !it.expanded) else it })
                    },
                    onSelectTask = { project, task -> selectedTasks += project to task; state = state.select(project, task) },
                    onNewTask = { newTasks += it }, modifier = Modifier.width(260.dp),
                    sessionSlots = NoSessionSlots,
                )
            }
        }
        onNodeWithContentDescription("Task Fix login").performClick().assertIsSelected()
        onNodeWithContentDescription("Project Beta").performClick().assertIsSelected()
        // A collapsed card hides its New task row; expanding Beta shows its own, apart from Alpha's.
        onNodeWithContentDescription("New task in Beta").assertDoesNotExist()
        onNodeWithContentDescription("Expand Beta").performClick()
        onNodeWithContentDescription("New task in Beta").performClick()
        onNodeWithText("Claude").performClick()
        onNodeWithContentDescription("New task in Alpha").performClick()
        onNodeWithText("Claude").performClick()
        runOnIdle {
            assertEquals(listOf("alpha" to "task-1"), selectedTasks)
            assertEquals(listOf("beta", "alpha"), newTasks)
        }
    }

    @Test
    fun closingAProjectWithNothingRunningClosesAtOnce() = runDesktopComposeUiTest {
        val closed = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    entryState(), onOpenProject = {}, onSelectProject = {}, onToggleProject = {},
                    onSelectTask = { _, _ -> }, onNewTask = {}, modifier = Modifier.width(260.dp),
                    onCloseProject = { closed += it },
                    sessionSlots = NoSessionSlots,
                )
            }
        }
        onNodeWithContentDescription("Actions for Alpha").performClick()
        onNodeWithText("Close Alpha").performClick()
        runOnIdle { assertEquals(listOf("alpha"), closed) }
    }

    @Test
    fun closingAProjectWithRunningAgentsAsksFirstAndKeepingItOpenClosesNothing() = runDesktopComposeUiTest {
        val closed = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectTaskSidebar(
                    entryState(), onOpenProject = {}, onSelectProject = {}, onToggleProject = {},
                    onSelectTask = { _, _ -> }, onNewTask = {}, modifier = Modifier.width(260.dp),
                    onCloseProject = { closed += it }, runningSessions = mapOf("alpha" to 2),
                    sessionSlots = NoSessionSlots,
                )
            }
        }
        onNodeWithContentDescription("Actions for Alpha").performClick()
        onNodeWithText("Close Alpha").performClick()
        onNodeWithText("2 agents are still running. Closing stops them.").assertExists()
        onNodeWithText("Keep open").performClick()
        onNodeWithText("2 agents are still running. Closing stops them.").assertDoesNotExist()
        runOnIdle { assertEquals(emptyList(), closed) }

        onNodeWithContentDescription("Actions for Alpha").performClick()
        onNodeWithText("Close Alpha").performClick()
        onNodeWithText("Stop and close").performClick()
        runOnIdle { assertEquals(listOf("alpha"), closed) }
    }

    @Test
    fun eachChoiceStartsItsOwnTerminalAtOnceWithNoRequestToFillIn() = runDesktopComposeUiTest {
        val submissions = mutableListOf<AgentRuntime>()
        setContent {
            MoruTheme { NewTaskComposer({ runtime -> submissions += runtime }, {}) }
        }
        onNodeWithText("Start task").assertDoesNotExist()
        listOf("Claude", "Codex", "AGY", "Terminal").forEach { name -> onNodeWithText(name).performClick() }
        runOnIdle {
            assertEquals(listOf(AgentRuntime.Claude, AgentRuntime.Codex, AgentRuntime.Agy, AgentRuntime.Terminal), submissions)
        }
    }

    @Test
    fun preparingTaskLocksTheChoicesAndShowsTheHostError() = runDesktopComposeUiTest {
        setContent {
            MoruTheme {
                NewTaskComposer({ error("Unexpected start") }, {}, isStarting = true, errorMessage = "Unable to prepare the workspace.")
            }
        }
        onNodeWithText("Claude").assertIsNotEnabled()
        onNodeWithText("Terminal").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithText("Preparing task…").assertExists()
        onNodeWithText("Unable to prepare the workspace.").assertExists()
    }

    private fun entryState(): ProjectTaskSidebarState {
        val task = TaskRow("task-1", "alpha", "Alpha", "Fix login", TaskDisplayState.Working, TaskGroup.Running, TaskAttention.None, true, false)
        return ProjectTaskSidebarState(
            listOf(
                ProjectRow("alpha", "Alpha", "/projects/alpha", null, true, true, true, listOf(task)),
                ProjectRow("beta", "Beta", "/projects/beta", null, true, false, false, emptyList()),
            ), "alpha", null,
        )
    }

    private fun ProjectTaskSidebarState.select(projectId: String, taskId: String?) = copy(
        selectedProjectId = projectId,
        selectedTaskId = taskId,
        projects = projects.map { project ->
            project.copy(selected = project.id == projectId, tasks = project.tasks.map { it.copy(selected = it.id == taskId) })
        },
    )
}
