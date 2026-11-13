package app.thdev.moru.app

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.data.InMemoryWorkbenchLayoutRepository
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.feature.sessions.SessionBoard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ViewedSessionResponseTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusedWorkOpeningMovesTheResponseToWaitingAndANewCompletionReappears() {
        val id = "viewed-board-response"
        var activity by mutableStateOf(SessionActivity.Working)
        var focused by mutableStateOf(true)
        try {
            runDesktopComposeUiTest {
                setContent {
                    val revision by SessionResponseAttention.revision.collectAsState()
                    val projected = remember(activity, revision) { SessionResponseAttention.project(id, activity) }
                    val rows = listOf(row(id).copy(activity = projected))
                    MoruTheme {
                        ObserveViewedSessionResponse(rows, "task", workVisible = true, windowFocused = focused)
                        SessionBoard(rows, mapOf("task" to SessionTaskRef("Fix login", "project", "Moru")),
                            onSelectSession = { response, _ ->
                                ViewedSessionResponseObserver.requestOpening(response.sessionId)
                            }, onOpenPreview = { _, _ -> })
                    }
                }
                onNodeWithText("Running").assertExists()
                runOnIdle { activity = SessionActivity.Quiet }
                onNodeWithText("Response ready").assertExists()
                runOnIdle { focused = false }
                onNodeWithText("Response ready").assertExists()
                runOnIdle { focused = true }
                onNodeWithText("Waiting").assertExists()
                onNodeWithText("Response ready").assertDoesNotExist()
                // Output-only activity after opening is a redraw, not another agent turn.
                runOnIdle { activity = SessionActivity.Working }
                onNodeWithText("Waiting").assertExists()
                runOnIdle { activity = SessionActivity.Quiet }
                onNodeWithText("Waiting").assertExists()
                runOnIdle {
                    SessionResponseAttention.observe(id, SessionActivity.Working)
                    activity = SessionActivity.Working
                }
                onNodeWithText("Running").assertExists()
                runOnIdle { activity = SessionActivity.Quiet }
                onNodeWithText("Response ready").assertExists()
                onNodeWithTag("session-row:$id").performClick()
                onNodeWithText("Waiting").assertExists()
            }
        } finally {
            SessionResponseAttention.forget(id)
        }
    }

    private fun row(id: String = "viewed-response", task: String = "task") = AgentSessionRow(
        sessionId = id, title = "Claude", agent = AgentRuntime.Named("Claude"), activity = SessionActivity.Quiet,
        lastOutputAtMillis = 1_000L, nowMillis = 10_000L, exitCode = null, previewUrl = null, selected = true, taskKey = task,
    )

    @Test
    fun workPlaceholdersCannotAcknowledgeEvenAnExplicitCompletedRowOpening() {
        fun visible(
            ownWindow: Boolean = false,
            available: Boolean = true,
            task: Boolean = true,
            composing: Boolean = false,
            preparing: Boolean = false,
            failed: Boolean = false,
            workTab: Boolean = true,
        ) = workbenchSessionSurfaceVisible(ownWindow, available, task, composing, preparing, failed, workTab)

        val exclusions = listOf(
            "another project window" to visible(ownWindow = true),
            "unavailable project" to visible(available = false),
            "no selected task" to visible(task = false),
            "new task composer" to visible(composing = true),
            "preparing launch" to visible(preparing = true),
            "failed launch" to visible(failed = true),
            "Code" to visible(workTab = false),
            "Git" to visible(workTab = false),
        )
        assertTrue(visible())
        for ((label, surfaceVisible) in exclusions) {
            val id = "excluded-response-$label"
            try {
                SessionResponseAttention.observe(id, SessionActivity.Quiet)
                assertFalse(surfaceVisible, label)
                assertNull(ViewedSessionResponseObserver().openedSession(id, surfaceVisible, true, explicitOpening = 1L), label)
                assertFalse(acknowledgeViewedSessionResponse(listOf(row(id)), id, "task", surfaceVisible, true), label)
                assertEquals(SessionActivity.Quiet, SessionResponseAttention.observe(id, SessionActivity.Quiet), label)
            } finally {
                SessionResponseAttention.forget(id)
            }
        }
    }

    @Test
    fun anOwningProjectInAnotherWindowRemainsUnreadUntilItsSessionsAreActuallyShownHere() {
        val windows = ProjectWindows(WorkbenchLayoutKeeper(InMemoryWorkbenchLayoutRepository()))
        val id = "other-window-completed-response"
        fun visible() = workbenchSessionSurfaceVisible(
            projectInOwnWindow = windows.isOpen("project"), projectAvailable = true, taskSelected = true,
            composing = false, preparing = false, failedLaunch = false,
        )
        try {
            SessionResponseAttention.observe(id, SessionActivity.Quiet)
            windows.open("project")
            val observer = ViewedSessionResponseObserver()
            assertNull(observer.openedSession(id, visible(), true, explicitOpening = 1L))
            assertFalse(acknowledgeViewedSessionResponse(listOf(row(id)), id, "task", visible(), true))
            assertEquals(SessionActivity.Quiet, SessionResponseAttention.observe(id, SessionActivity.Quiet))
            windows.close("project")
            assertEquals(id, observer.openedSession(id, visible(), true, explicitOpening = 1L))
            assertTrue(acknowledgeViewedSessionResponse(listOf(row(id)), id, "task", visible(), true))
            assertEquals(SessionActivity.Waiting, SessionResponseAttention.observe(id, SessionActivity.Quiet))
        } finally {
            SessionResponseAttention.forget(id)
        }
    }

    @Test
    fun anUnrelatedOpeningRequestDoesNotReopenTheAlreadyVisibleSession() {
        val observer = ViewedSessionResponseObserver()
        assertEquals("one", observer.openedSession("one", true, true, explicitOpening = 5L))
        assertNull(observer.openedSession("one", true, true, explicitOpening = 0L))
        assertNull(observer.openedSession("one", true, true, explicitOpening = 5L))
        assertEquals("one", observer.openedSession("one", true, true, explicitOpening = 6L))
    }

    @Test
    fun backgroundSelectionAndCodeOrGitDoNotOpenAResponse() {
        val observer = ViewedSessionResponseObserver()
        assertNull(observer.openedSession("one", workVisible = true, windowFocused = false))
        assertNull(observer.openedSession("one", workVisible = false, windowFocused = true))
        assertEquals("one", observer.openedSession("one", workVisible = true, windowFocused = true))
    }

    @Test
    fun finishingTheAlreadyVisibleRunningSessionDoesNotCreateAnotherOpening() {
        val observer = ViewedSessionResponseObserver()
        val id = "viewed-running-response"
        try {
            SessionResponseAttention.observe(id, SessionActivity.Working)
            assertEquals(id, observer.openedSession(id, true, true))
            assertFalse(acknowledgeViewedSessionResponse(listOf(row(id).copy(activity = SessionActivity.Working)), id, "task", true, true))
            SessionResponseAttention.observe(id, SessionActivity.Quiet)
            assertNull(observer.openedSession(id, true, true))
            assertEquals(SessionActivity.Quiet, SessionResponseAttention.observe(id, SessionActivity.Quiet))
            // Selecting the same completed row or its terminal after completion is a deliberate opening.
            assertTrue(acknowledgeViewedSessionResponse(listOf(row(id)), id, "task", true, true))
            assertEquals(SessionActivity.Waiting, SessionResponseAttention.observe(id, SessionActivity.Quiet))
        } finally {
            SessionResponseAttention.forget(id)
        }
    }

    @Test
    fun returningToWorkOrRefocusingTheWindowOpensTheVisibleResponse() {
        val observer = ViewedSessionResponseObserver()
        assertEquals("one", observer.openedSession("one", true, true))
        assertNull(observer.openedSession("one", false, true))
        assertEquals("one", observer.openedSession("one", true, true))
        assertNull(observer.openedSession("one", true, false))
        assertEquals("one", observer.openedSession("one", true, true))
        assertEquals("two", observer.openedSession("two", true, true))
        assertNull(observer.openedSession("two", true, true))
    }

    @Test
    fun acknowledgmentRequiresTheActualFocusedWorkTaskAndNamedCompletedSession() {
        val response = row()
        try {
            SessionResponseAttention.observe(response.sessionId, SessionActivity.Quiet)
            assertFalse(acknowledgeViewedSessionResponse(listOf(response), response.sessionId, "other-task", true, true))
            assertFalse(acknowledgeViewedSessionResponse(listOf(response), response.sessionId, "task", false, true))
            assertFalse(acknowledgeViewedSessionResponse(listOf(response), response.sessionId, "task", true, false))
            assertFalse(acknowledgeViewedSessionResponse(listOf(response.copy(agent = AgentRuntime.Terminal)), response.sessionId, "task", true, true))
            assertTrue(acknowledgeViewedSessionResponse(listOf(response), response.sessionId, "task", true, true))
            assertEquals(SessionActivity.Waiting, SessionResponseAttention.observe(response.sessionId, SessionActivity.Quiet))
        } finally {
            SessionResponseAttention.forget(response.sessionId)
        }
    }
}
