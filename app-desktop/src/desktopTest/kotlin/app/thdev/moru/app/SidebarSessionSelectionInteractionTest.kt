package app.thdev.moru.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.terminal.TerminalSessionSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Direct sidebar selection updates both highlights before the 250 ms external observer can advance. */
@OptIn(ExperimentalTestApi::class)
class SidebarSessionSelectionInteractionTest {
    @Test
    fun selectingAnotherTasksSessionAndAnotherSessionInTheSameTaskNeedsNoPoll() = ProjectToolbarFixture().use { fixture ->
        val alphaSecondTask = fixture.catalog.createTask(fixture.projectA.id, "Alpha second task", "Read Alpha 2")
        val first = fixture.sessions.createSession(TerminalSessionSpec(title = "Alpha session", attachedItemId = fixture.taskA.id))
        val second = fixture.sessions.createSession(TerminalSessionSpec(title = "Alpha second first", attachedItemId = alphaSecondTask.id))
        val third = fixture.sessions.createSession(TerminalSessionSpec(title = "Alpha second second", attachedItemId = alphaSecondTask.id))
        fixture.sessions.selectSession(first.id)
        runDesktopComposeUiTest(width = 1100, height = 700) {
            setContent { fixture.App() }
            onNodeWithTag("session-row:${first.id}").assertIsSelected()
            mainClock.autoAdvance = false
            val before = mainClock.currentTime
            onNodeWithTag("session-row:${third.id}").performClick()
            mainClock.advanceTimeByFrame()
            onNodeWithContentDescription("Task Alpha second task").assertIsSelected()
            onNodeWithTag("session-row:${third.id}").assertIsSelected()
            onNodeWithTag("session-row:${first.id}").assertIsNotSelected()
            onNodeWithTag("session-row:${second.id}").performClick()
            mainClock.advanceTimeByFrame()
            onNodeWithTag("session-row:${second.id}").assertIsSelected()
            onNodeWithTag("session-row:${third.id}").assertIsNotSelected()
            assertTrue(mainClock.currentTime - before < 250, "Direct selection must not wait for the revision observer")
            runOnIdle { assertEquals(second.id, fixture.sessions.selectedSessionId()) }
        }
    }

    @Test
    fun byDefaultSelectingAnotherProjectsSessionKeepsThePreviousProjectExpanded() = ProjectToolbarFixture().use { fixture ->
        val first = fixture.sessions.createSession(TerminalSessionSpec(title = "Alpha session", attachedItemId = fixture.taskA.id))
        val betaSession = fixture.sessions.createSession(TerminalSessionSpec(title = "Beta session", attachedItemId = fixture.taskB.id))
        fixture.sessions.selectSession(first.id)
        runDesktopComposeUiTest(width = 1100, height = 700) {
            setContent { fixture.App() }
            onNodeWithTag("session-row:${first.id}").assertIsSelected()
            onNodeWithTag("session-row:${betaSession.id}").performClick()
            onNodeWithContentDescription("Task Beta task").assertIsSelected()
            onNodeWithTag("session-row:${betaSession.id}").assertIsSelected()
            onNodeWithTag("session-row:${first.id}").assertIsNotSelected()
        }
    }

    @Test
    fun whenAutoCollapseIsEnabledSelectingAnotherProjectsSessionCollapsesThePreviousProject() = ProjectToolbarFixture().use { fixture ->
        fixture.generalSettings = fixture.generalSettings.copy(autoCollapseInactiveSidebar = true)
        val first = fixture.sessions.createSession(TerminalSessionSpec(title = "Alpha session", attachedItemId = fixture.taskA.id))
        val betaSession = fixture.sessions.createSession(TerminalSessionSpec(title = "Beta session", attachedItemId = fixture.taskB.id))
        fixture.sessions.selectSession(first.id)
        runDesktopComposeUiTest(width = 1100, height = 700) {
            setContent { fixture.App() }
            onNodeWithTag("session-row:${first.id}").assertIsSelected()
            onNodeWithContentDescription("Expand Beta").performClick()
            onNodeWithTag("session-row:${betaSession.id}").performClick()
            onNodeWithContentDescription("Task Beta task").assertIsSelected()
            onNodeWithTag("session-row:${betaSession.id}").assertIsSelected()
            onNodeWithTag("session-row:${first.id}").assertDoesNotExist()
        }
    }
}
