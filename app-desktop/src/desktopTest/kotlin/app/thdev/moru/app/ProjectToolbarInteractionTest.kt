package app.thdev.moru.app

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.terminal.TerminalSessionSpec
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ProjectToolbarInteractionTest {
    @Test
    fun toolbarSharesTheTitleRowAndProjectSelectionWithSidebarAndStatusBar() = ProjectToolbarFixture().use { fixture ->
        runDesktopComposeUiTest(width = 1100, height = 700) {
            setContent { fixture.App() }
            onNodeWithText("Workspace").assertDoesNotExist()
            val selector = onNodeWithContentDescription("Switch project").getBoundsInRoot()
            val git = onNodeWithText("Git").getBoundsInRoot()
            assertTrue(selector.top < 40.dp && selector.bottom <= 40.dp)
            assertTrue(git.top < 40.dp && git.bottom <= 40.dp)
            onNodeWithContentDescription("Branch alpha-branch").assertIsDisplayed()
            onNodeWithContentDescription("Path ${fixture.alpha.canonicalPath}").assertIsDisplayed()
            capture("project-toolbar-light")
            onNodeWithContentDescription("Switch project").performClick()
            onNodeWithText("Open projects").assertExists()
            onNodeWithText("Recent projects").assertExists()
            onNodeWithContentDescription("Switch to project Beta").performClick()
            onNodeWithText("Open projects").assertDoesNotExist()
            onNodeWithContentDescription("Path ${fixture.beta.canonicalPath}").assertIsDisplayed()
            onNodeWithContentDescription("Branch alpha-branch").assertDoesNotExist()
            runOnIdle {
                assertEquals(fixture.projectB.id, fixture.catalog.loadCatalog().selectedProjectId)
                assertEquals(fixture.beta.canonicalPath, fixture.selectedRoot)
                assertTrue(fixture.sessions.listSessions().isEmpty())
            }
            onNodeWithContentDescription("Switch project").performClick()
            onNodeWithContentDescription("Reopen project Recent").performClick()
            onNodeWithContentDescription("Path ${fixture.recent.canonicalPath}").assertIsDisplayed()
            runOnIdle { assertEquals(fixture.projectRecent.id, fixture.catalog.loadCatalog().selectedProjectId) }
        }
    }

    @Test
    fun sessionsInsideTheProjectMenuNavigateToTheirOwningTaskWithoutRecreatingSessions() = ProjectToolbarFixture().use { fixture ->
        val session = fixture.sessions.createSession(TerminalSessionSpec(title = "Beta terminal",
            workingDirectory = fixture.beta.canonicalPath, attachedItemId = fixture.taskB.id))
        runDesktopComposeUiTest(width = 800, height = 700) {
            setContent { fixture.App(dark = true) }
            onNodeWithText("Git").assertIsDisplayed()
            capture("project-toolbar-narrow-dark")
            onNodeWithContentDescription("Switch project").performClick()
            onNodeWithText("Sessions").performClick()
            onNodeWithText("Beta · Beta task").assertExists().performClick()
            onNodeWithText("‹ Projects").assertDoesNotExist()
            onNodeWithContentDescription("Path ${fixture.beta.canonicalPath}").assertIsDisplayed()
            runOnIdle {
                assertEquals(fixture.projectB.id, fixture.catalog.loadCatalog().selectedProjectId)
                assertEquals(fixture.taskB.id, fixture.catalog.loadCatalog().selectedTaskId)
                assertEquals(listOf(session.id), fixture.sessions.listSessions().map { it.id })
                assertEquals(session.id, fixture.sessions.selectedSessionId())
            }
            onNodeWithContentDescription("Switch project").performClick()
            onNodeWithText("Sessions").performClick()
            onNodeWithText("‹ Projects").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .performKeyInput { pressKey(Key.Escape) }
            onNodeWithText("‹ Projects").assertDoesNotExist()
        }
    }

    @Test
    fun openingFromMenuKeepsCancellationAndPickerFailureRecoverable() = ProjectToolbarFixture().use { fixture ->
        runDesktopComposeUiTest(width = 900, height = 700) {
            setContent { fixture.App() }
            onNodeWithContentDescription("Switch project").performClick()
            onNodeWithText("Open project…").performClick()
            runOnIdle { assertEquals(fixture.projectA.id, fixture.catalog.loadCatalog().selectedProjectId); fixture.pickerFailure = true }
            onNodeWithContentDescription("Switch project").performClick()
            onNodeWithText("Open project…").performClick()
            onNodeWithText("Picker unavailable", substring = true).assertIsDisplayed()
            runOnIdle { assertEquals(fixture.projectA.id, fixture.catalog.loadCatalog().selectedProjectId) }
        }
    }

    private fun ComposeUiTest.capture(name: String) {
        val output = File("build/ui-captures/$name.png")
        output.parentFile.mkdirs()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", output)
    }
}
