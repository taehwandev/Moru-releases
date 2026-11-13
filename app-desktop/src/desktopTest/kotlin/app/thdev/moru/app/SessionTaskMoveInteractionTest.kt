package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.domain.WorkbenchLayoutRepository
import app.thdev.moru.core.model.WorkbenchLayout
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.graphics.toAwtImage
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class SessionTaskMoveInteractionTest {
    @Test
    fun sidebarMenuMovesTheExistingSessionAndSelectsItsDestination() {
        ProjectToolbarFixture().use { f ->
            val target = f.catalog.createTask(f.projectA.id, "Move destination", "Keep destination request")
            val session = f.sessions.createSession(TerminalSessionSpec(title = "Working session", attachedItemId = f.taskA.id,
                workingDirectory = f.alpha.path, hostKey = "held-session"))
            runDesktopComposeUiTest(width = 1100, height = 760) {
                setContent { f.App() }
                onNodeWithTag("session-actions:${session.id}", useUnmergedTree = true).performClick()
                onNodeWithText("Move to Task…").performClick()
                onNodeWithTag("session-move-target:${f.taskB.id}").assertDoesNotExist()
                onNodeWithTag("session-move-target:${f.taskA.id}").assertDoesNotExist()
                onNodeWithText("Move", substring = false).assertIsNotEnabled()
                onNodeWithTag("session-move-target:${target.id}").performClick()
                onNodeWithText("Move", substring = false).performClick()
                onNodeWithTag("session-move-dialog").assertDoesNotExist()
                runOnIdle {
                    assertEquals(target.id, f.catalog.loadCatalog().selectedTaskId)
                    assertEquals(session.id, f.sessions.selectedSessionId())
                    assertEquals(target.id, f.sessions.listSessions().first { it.id == session.id }.spec.attachedItemId)
                    assertEquals("held-session", f.sessions.listSessions().first { it.id == session.id }.spec.hostKey)
                }
            }
        }
    }

    @Test
    fun cancelAndEscapeLeaveOwnershipUnchangedAndSuccessfulMoveIsSavedImmediately() {
        ProjectToolbarFixture().use { f ->
            val target = f.catalog.createTask(f.projectA.id, "Destination with a longer task title", "")
            val session = f.sessions.createSession(TerminalSessionSpec(title = "Claude conversation", attachedItemId = f.taskA.id,
                workingDirectory = f.alpha.path, hostKey = "stable-host"))
            val navigation = WorkbenchNavigation(f.catalog)
            var saved = WorkbenchLayout()
            val keeper = WorkbenchLayoutKeeper(object : WorkbenchLayoutRepository {
                override fun loadLayout() = saved
                override fun saveLayout(layout: WorkbenchLayout) { saved = layout }
            })
            var revision by mutableLongStateOf(f.sessions.revision())
            runDesktopComposeUiTest(width = 620, height = 600) {
                setContent { MoruTheme(darkTheme = true) {
                    WithSessionTaskMoves(navigation, f.sessions, revision, keeper, onMoved = { revision = f.sessions.revision() }) {
                        val request = LocalSessionTaskMove.current!!
                        SessionTaskMoveMenu("request", session.title) { request(session.id) }
                    }
                } }
                fun open() {
                    onNodeWithTag("session-actions:request").performClick()
                    onNodeWithText("Move to Task…").performClick()
                }
                open()
                onNodeWithText("Cancel").performClick()
                onNodeWithTag("session-move-dialog").assertDoesNotExist()
                open()
                onNodeWithTag("session-move-dialog").performKeyInput { pressKey(Key.Escape) }
                onNodeWithTag("session-move-dialog").assertDoesNotExist()
                runOnIdle { assertEquals(f.taskA.id, f.sessions.listSessions().single().spec.attachedItemId) }
                open()
                onNodeWithTag("session-move-target:${target.id}").performClick()
                System.getenv("MORU_SESSION_MOVE_SHOT_DIR")?.let { dir ->
                    val output = File(dir, "move-session-dark-narrow.png")
                    ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", output)
                }
                onNodeWithText("Move", substring = false).performClick()
                runOnIdle {
                    keeper.flush()
                    assertEquals(target.id, saved.terminals.single().taskId)
                    assertEquals("stable-host", saved.terminals.single().hostKey)
                    assertEquals(0, saved.selectedTerminal)
                }
            }
        }
    }

    @Test
    fun emptyAndFullTargetsCannotBeMovedAndStaleTargetShowsAnError() {
        ProjectToolbarFixture().use { f ->
            val session = f.sessions.createSession(TerminalSessionSpec(attachedItemId = f.taskA.id))
            val navigation = WorkbenchNavigation(f.catalog)
            var revision by mutableLongStateOf(f.sessions.revision())
            runDesktopComposeUiTest(width = 620, height = 600) {
                setContent { MoruTheme {
                    WithSessionTaskMoves(navigation, f.sessions, revision, null, onMoved = { revision = f.sessions.revision() }) {
                        val request = LocalSessionTaskMove.current!!
                        SessionTaskMoveMenu(session.id, session.title) { request(session.id) }
                    }
                } }
                fun open() {
                    onNodeWithTag("session-actions:${session.id}").performClick()
                    onNodeWithText("Move to Task…").performClick()
                }
                open()
                onNodeWithText("No other Task is available in this Project. Create a Task first.").assertExists()
                onNodeWithText("Move", substring = false).assertIsNotEnabled()
                onNodeWithText("Cancel").performClick()
                val full = f.catalog.createTask(f.projectA.id, "Full destination", "")
                repeat(TaskTerminalSessionCapacity.Maximum) { f.sessions.createSession(TerminalSessionSpec(attachedItemId = full.id)) }
                runOnIdle { revision = f.sessions.revision() }
                open()
                onNodeWithTag("session-move-target:${full.id}").assertIsNotEnabled()
                onNodeWithText("Cancel").performClick()
                val gone = f.catalog.createTask(f.projectA.id, "Will disappear", "")
                open()
                onNodeWithTag("session-move-target:${gone.id}").performClick()
                runOnIdle { f.catalog.deleteTask(gone.id) }
                onNodeWithText("Move", substring = false).performClick()
                onNodeWithText("Choose an available Task in the same Project. The Task list may have changed.").assertExists()
                runOnIdle { assertEquals(f.taskA.id, f.sessions.listSessions().first { it.id == session.id }.spec.attachedItemId) }
            }
        }
    }
}
