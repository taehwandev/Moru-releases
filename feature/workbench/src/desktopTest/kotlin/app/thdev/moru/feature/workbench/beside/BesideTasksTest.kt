package app.thdev.moru.feature.workbench.beside

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.panel.PanelLayouts
import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.core.model.panel.PanelSplit
import app.thdev.moru.core.ui.identity.taskIdentityAccent
import app.thdev.moru.feature.workbench.TaskRowActions
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.clickable
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.SessionPermissionChoice
import app.thdev.moru.core.terminal.SessionPermissionPrompt
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.feature.workbench.slots.WorkbenchTerminalSlots
import app.thdev.moru.feature.workbench.slots.TerminalPaneSpec
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskRowView
import app.thdev.moru.feature.workbench.screenshot.AttentionFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BesideTasksTest {
    private val other = BesideTask("task-b", "Task B", "/work/b")

    @Test
    fun withNoTaskBesideTheSelectedTaskTakesTheWholeArea() = runDesktopComposeUiTest(width = 1200, height = 600) {
        setContent {
            MoruTheme {
                BesideTasksLayout(emptyList(), null, {}, Modifier.size(1200.dp, 600.dp),
                    main = { Box(it.testTag("main")) }, beside = { _, m, _, _ -> Box(m.testTag("beside")) })
            }
        }
        assertEquals(1200f, onNodeWithTag("main").fetchSemanticsNode().boundsInRoot.width, 1f)
        onNodeWithTag("beside").assertDoesNotExist()
    }

    @Test
    fun aTaskBesideTakesItsSavedShareAndTheBorderMovesIt() = runDesktopComposeUiTest(width = 1200, height = 600) {
        var split by mutableStateOf<PanelNode?>(PanelLayouts.resize(PanelLayouts.row(2), emptyList(), 0, 0.6f))
        setContent {
            MoruTheme {
                BesideTasksLayout(listOf(other), split, { split = it }, Modifier.size(1200.dp, 600.dp),
                    main = { Box(it.testTag("main")) }, beside = { _, m, _, _ -> Box(m.testTag("beside")) })
            }
        }
        assertEquals(720f, onNodeWithTag("main").fetchSemanticsNode().boundsInRoot.width, 1f)
        assertEquals(480f, onNodeWithTag("beside").fetchSemanticsNode().boundsInRoot.width, 1f)
        val seam = onNodeWithContentDescription("Resize Tasks beside")
        seam.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        seam.performKeyInput { pressKey(Key.DirectionLeft) }
        runOnIdle { assertEquals(0.58f, (split as PanelSplit).shares[0], 0.001f) }
    }

    @Test
    fun onlyAnotherTaskOfTheSelectedTasksProjectOffersToOpenBeside() = runDesktopComposeUiTest {
        val task = AttentionFixtures.sidebarState().projects.first().tasks.first()
        val other = task.copy(id = "other", title = "Other task", selected = false)
        val current = task.copy(id = "current", title = "Current task", selected = true)
        val elsewhere = task.copy(id = "elsewhere", projectId = "another-project", title = "Elsewhere task", selected = false)
        val toggled = mutableListOf<String>()
        var besideIds by mutableStateOf(emptySet<String>())
        setContent {
            MoruTheme {
                val actions = TaskRowActions(besideTaskIds = besideIds, besideProjectId = task.projectId, onToggleBeside = { toggled += it })
                Column {
                    TaskRowView(other, {}, actions, accent = taskIdentityAccent(other.id))
                    TaskRowView(current, {}, actions, accent = taskIdentityAccent(current.id))
                    TaskRowView(elsewhere, {}, actions, accent = taskIdentityAccent(elsewhere.id))
                }
            }
        }
        onNodeWithContentDescription("Actions for Other task").performClick()
        onNodeWithText("Open beside").performClick()
        runOnIdle { assertEquals(listOf("other"), toggled) }

        runOnIdle { besideIds = setOf("other") }
        onNodeWithContentDescription("Actions for Other task").performClick()
        onNodeWithText("Close beside").assertExists()
        onNodeWithText("Rename").performClick()

        // The selected Task is already shown, and another Project's Task belongs in that Project's own window.
        listOf("Current task", "Elsewhere task").forEach { title ->
            onNodeWithContentDescription("Actions for $title").performClick()
            onNodeWithText("Rename").assertExists()
            onNodeWithText("Open beside").assertDoesNotExist()
            onNodeWithText("Close beside").assertDoesNotExist()
            onNodeWithText("Rename").performClick()
        }
    }

    @Test
    fun draggingTheTaskBesideOntoTheSelectedTaskTradesThem() = runDesktopComposeUiTest(width = 1200, height = 600) {
        val traded = mutableListOf<String>()
        setContent {
            MoruTheme {
                BesideTasksLayout(listOf(other), null, {}, Modifier.size(1200.dp, 600.dp),
                    main = { Box(it.testTag("main")) },
                    beside = { task, m, header, _ ->
                        Column(m) { Box(header.fillMaxWidth().height(28.dp).testTag("header-${task.id}")) }
                    },
                    mainTitle = "Task A",
                    mainState = TaskDisplayState.Working,
                    onDropOnMain = { traded += it },
                )
            }
        }
        // The selected Task is headed like the Task beside, so the two panes read alike.
        onNodeWithText("Task A").assertExists()
        onNodeWithTag("header-task-b").performMouseInput {
            moveTo(center); press()
            moveTo(center - Offset(300f, -100f), delayMillis = 50)
            moveTo(center - Offset(600f, -200f), delayMillis = 50)
            release()
        }
        runOnIdle { assertEquals(listOf("task-b"), traded) }
    }

    @Test
    fun theHeaderSaysWhatTheTaskIsDoingAndClickingItsTitleSwitchesToIt() = runDesktopComposeUiTest(width = 600, height = 400) {
        var state by mutableStateOf(TaskDisplayState.Working)
        var selected = 0
        val slots = WorkbenchTerminalSlots(pane = { _, _ -> }, typeInto = { _, _ -> false }, submitLine = { _, _ -> false })
        setContent {
            MoruTheme {
                BesideTaskPane(
                    task = other.copy(state = state),
                    spec = TerminalPaneSpec(
                        sessionManager = InMemoryTerminalSessionManager(),
                        defaultWorkingDirectory = other.folder,
                        defaultShell = "/bin/sh",
                        workspaceId = other.id,
                        workspaceName = other.title,
                        terminalFontFamily = null,
                        terminalFontSize = 13,
                        terminalScrollbackLines = 100,
                        onOpenExternalTerminal = {},
                        onNewTerminal = null,
                    ),
                    slots = slots,
                    onClose = {},
                    onSelect = { selected++ },
                    modifier = Modifier.size(600.dp, 400.dp),
                )
            }
        }
        onNodeWithText("Working").assertExists()
        runOnIdle { state = TaskDisplayState.NeedsApproval }
        onNodeWithText("Needs approval").assertExists()
        onNodeWithText("Task B").performClick()
        runOnIdle { assertEquals(1, selected) }
    }

    @Test
    fun aRequestFromATaskBesideIsAnsweredInItsPaneAndLeavesWithTheAnswer() = runDesktopComposeUiTest(width = 600, height = 500) {
        val sessions = InMemoryTerminalSessionManager()
        val asking = sessions.createSession(TerminalSessionSpec(title = "claude", attachedItemId = other.id, shell = "/bin/sh"))
        val elsewhere = sessions.createSession(TerminalSessionSpec(title = "other", attachedItemId = "task-c", shell = "/bin/sh"))
        var chosen: String? = null
        val slots = WorkbenchTerminalSlots(pane = { _, _ -> }, typeInto = { _, _ -> false }, submitLine = { _, _ -> false })
        setContent {
            MoruTheme {
                BesideTaskPane(
                    task = other.copy(state = TaskDisplayState.NeedsApproval),
                    spec = TerminalPaneSpec(
                        sessionManager = sessions,
                        defaultWorkingDirectory = other.folder,
                        defaultShell = "/bin/sh",
                        workspaceId = other.id,
                        workspaceName = other.title,
                        terminalFontFamily = null,
                        terminalFontSize = 13,
                        terminalScrollbackLines = 100,
                        onOpenExternalTerminal = {},
                        onNewTerminal = null,
                    ),
                    slots = slots,
                    onClose = {},
                    // Stands in for the shared controls: one button per choice that answers the live request.
                    permissionActions = { sessionId, modifier ->
                        val request = SessionPermissions.requests.collectAsState().value[sessionId]
                        if (request != null) Column(modifier) {
                            request.choices.forEach { choice ->
                                androidx.compose.foundation.layout.Box(
                                    Modifier.testTag("choice-$sessionId-${choice.id}")
                                        .clickable { SessionPermissions.respond(sessionId, request.id, choice.id) },
                                ) { androidx.compose.material3.Text(choice.label) }
                            }
                        }
                    },
                    modifier = Modifier.size(600.dp, 500.dp),
                )
            }
        }
        try {
            onNodeWithTag("choice-${asking.id}-allow").assertDoesNotExist()
            SessionPermissions.observe(asking.id, SessionPermissionPrompt("Allow command?", "pwd", listOf(
                SessionPermissionChoice("allow", "Allow once"), SessionPermissionChoice("deny", "Deny")))) { chosen = it; true }
            // A request from another Task's terminal is not this pane's to answer.
            SessionPermissions.observe(elsewhere.id, SessionPermissionPrompt("Allow?", "ls", listOf(
                SessionPermissionChoice("allow", "Allow once")))) { true }

            onNodeWithTag("choice-${asking.id}-allow").performClick()
            runOnIdle { assertEquals("allow", chosen) }
            onNodeWithTag("choice-${asking.id}-allow").assertDoesNotExist()
            onNodeWithTag("choice-${elsewhere.id}-allow").assertDoesNotExist()
        } finally {
            SessionPermissions.forget(asking.id)
            SessionPermissions.forget(elsewhere.id)
        }
    }
}
