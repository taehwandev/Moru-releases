package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.workbench.slots.WorkbenchTerminalSlots
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TaskTerminalPaneResumeTest {
    @Test
    fun switchingTasksChangesTheResumeButtonAndItsLaunchedConversation() = runDesktopComposeUiTest {
        val sessions = InMemoryTerminalSessionManager()
        listOf("task-1" to "claude --session-id c1", "task-2" to "codex --no-daemon resume x9").forEach { (task, command) ->
            val session = sessions.createSession(TerminalSessionSpec(attachedItemId = task, startupCommand = command))
            sessions.reportExit(session.id, 0)
        }
        setContent {
            MoruTheme {
                var task by remember { mutableStateOf("task-1") }
                Column {
                    MoruButton(text = "Select Task 2", onClick = { task = "task-2" })
                    TaskTerminalPane(
                        sessions = sessions, folder = "/work/shared", shell = "zsh", taskKey = task, taskName = task,
                        fontFamily = null, fontSize = 13, scrollbackLines = 100,
                        onOpenExternalTerminal = {}, taskAgent = null, onStartSession = { null },
                        slots = WorkbenchTerminalSlots(pane = { _, _ -> }, typeInto = { _, _ -> false }, submitLine = { _, _ -> false }),
                        modifier = Modifier.width(800.dp).height(400.dp),
                    )
                }
            }
        }

        waitUntil { onAllNodesWithText("Claude").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Codex").assertDoesNotExist()
        onNodeWithText("AGY").assertDoesNotExist()
        onNodeWithText("Select Task 2").performClick()
        waitUntil { onAllNodesWithText("Codex").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Claude").assertDoesNotExist()
        onNodeWithText("Codex").performClick()

        runOnIdle {
            val launched = sessions.listSessions().last()
            assertEquals("task-2", launched.spec.attachedItemId)
            assertEquals("codex --no-daemon resume x9", launched.spec.startupCommand)
            assertEquals("/work/shared", launched.spec.workingDirectory)
        }
    }
}
