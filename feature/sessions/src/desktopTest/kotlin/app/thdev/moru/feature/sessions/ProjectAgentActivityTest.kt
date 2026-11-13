package app.thdev.moru.feature.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.session.SessionTaskRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProjectAgentActivityTest {
    @Test
    fun onlyActiveAndCompletedAgentSessionsAreListedWithTheirTaskAndOpenOnClick() = runDesktopComposeUiTest {
        val rows = SessionActivity.entries.map { activity -> session(activity.name, activity) } +
            session("shell", SessionActivity.Working, AgentRuntime.Terminal) +
            session("quiet-shell", SessionActivity.Quiet, AgentRuntime.Terminal)
        val opened = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectAgentActivity(rows, mapOf("task" to SessionTaskRef("Fix login", "p", "Project")), { opened += it.sessionId })
            }
        }
        val shown = listOf("Asking", "Starting", "Working", "Quiet")
        shown.forEach { onNodeWithTag("project-agent-line:$it").assertExists() }
        (SessionActivity.entries.map { it.name } - shown.toSet() + listOf("shell", "quiet-shell")).forEach {
            onNodeWithTag("project-agent-line:$it").assertDoesNotExist()
        }
        onNodeWithTag("project-agent-line:Quiet").assertContentDescriptionEquals("Claude, Fix login, Session title, Response ready")
        onNodeWithTag("project-agent-line:Asking").performClick()
        runOnIdle { assertEquals(listOf("Asking"), opened) }
    }

    @Test
    fun aCompletedOnlyProjectStaysVisibleAndOpensItsSession() = runDesktopComposeUiTest {
        val completed = session("done", SessionActivity.Quiet)
        val opened = mutableListOf<AgentSessionRow>()
        setContent {
            MoruTheme {
                ProjectAgentActivity(listOf(completed), emptyMap(), { opened += it })
            }
        }
        onNodeWithTag("project-agent-activity").assertExists()
        onNodeWithTag("project-agent-line:done")
            .assertContentDescriptionEquals("Claude, Sessions without a Task, Session title, Response ready")
            .performClick()
        runOnIdle { assertEquals(listOf(completed), opened) }
    }

    @Test
    fun aWorkingSessionRemainsVisibleWhenItsTurnCompletes() = runDesktopComposeUiTest {
        var row by mutableStateOf(session("session", SessionActivity.Working))
        val opened = mutableListOf<AgentSessionRow>()
        setContent {
            MoruTheme {
                ProjectAgentActivity(listOf(row), emptyMap(), { opened += it })
            }
        }
        onNodeWithTag("project-agent-line:session").assertExists()
        runOnIdle { row = row.copy(activity = SessionActivity.Quiet) }
        onNodeWithTag("project-agent-line:session")
            .assertContentDescriptionEquals("Claude, Sessions without a Task, Session title, Response ready")
            .performClick()
        runOnIdle { assertEquals(listOf(row), opened) }
    }

    @Test
    fun askingAndWorkingGetPreviewSlotsWhileRowsRetainTheirSourceOrder() = runDesktopComposeUiTest {
        val rows = listOf(
            session("done1", SessionActivity.Quiet),
            session("working", SessionActivity.Working),
            session("done2", SessionActivity.Quiet),
            session("asking", SessionActivity.Asking),
            session("starting", SessionActivity.Starting),
            session("done3", SessionActivity.Quiet),
        )
        setContent {
            MoruTheme {
                ProjectAgentActivity(rows, emptyMap(), {})
            }
        }
        val shown = listOf("done1", "working", "asking", "starting")
        shown.forEach { onNodeWithTag("project-agent-line:$it").assertExists() }
        shown.zipWithNext().forEach { (before, after) ->
            val beforeBounds = onNodeWithTag("project-agent-line:$before").fetchSemanticsNode().boundsInRoot
            val afterBounds = onNodeWithTag("project-agent-line:$after").fetchSemanticsNode().boundsInRoot
            assertTrue(beforeBounds.bottom <= afterBounds.top, "$before must be listed before $after")
        }
        listOf("done2", "done3").forEach {
            onNodeWithTag("project-agent-line:$it").assertDoesNotExist()
        }
        onNodeWithTag("project-agent-overflow").assertTextEquals("+2")
    }

    @Test
    fun completingAPreviewDoesNotReorderItsTaskOrSiblingSessions() = runDesktopComposeUiTest {
        var rows by mutableStateOf(listOf(
            session("a", SessionActivity.Working).copy(taskKey = "first"),
            session("b", SessionActivity.Working).copy(taskKey = "first"),
            session("c", SessionActivity.Working).copy(taskKey = "second"),
        ))
        setContent {
            MoruTheme {
                ProjectAgentActivity(rows, linkedMapOf(
                    "first" to SessionTaskRef("First task", "p", "Project"),
                    "second" to SessionTaskRef("Second task", "p", "Project"),
                ), {})
            }
        }
        val before = listOf("a", "b", "c").associateWith {
            onNodeWithTag("project-agent-line:$it").fetchSemanticsNode().boundsInRoot.top
        }
        runOnIdle { rows = rows.map { if (it.sessionId == "a") it.copy(activity = SessionActivity.Quiet) else it } }
        before.forEach { (id, top) ->
            assertEquals(top, onNodeWithTag("project-agent-line:$id").fetchSemanticsNode().boundsInRoot.top)
        }
    }

    @Test
    fun waitingIdleAndEndedSessionsDrawNothing() = runDesktopComposeUiTest {
        setContent {
            MoruTheme {
                ProjectAgentActivity(
                    listOf(SessionActivity.Waiting, SessionActivity.Idle, SessionActivity.Exited)
                        .map { session(it.name, it) },
                    emptyMap(),
                    {},
                )
            }
        }
        onNodeWithTag("project-agent-activity").assertDoesNotExist()
    }

    @Test
    fun repeatedSessionsShareATaskHeadingWhileDuplicateTaskTitlesRemainDistinct() = runDesktopComposeUiTest {
        val rows = listOf(session("a1", SessionActivity.Working).copy(taskKey = "a"),
            session("a2", SessionActivity.Quiet).copy(taskKey = "a"),
            session("b1", SessionActivity.Working).copy(taskKey = "b"))
        val opened = mutableListOf<String>()
        setContent {
            MoruTheme {
                ProjectAgentActivity(rows, mapOf("a" to SessionTaskRef("Same task", "p", "Project"),
                    "b" to SessionTaskRef("Same task", "p", "Project")), { opened += it.sessionId })
            }
        }
        onAllNodesWithText("Same task").assertCountEquals(2)
        listOf("a1" to "a", "a2" to "a", "b1" to "b").forEach { (session, task) ->
            onNode(hasTestTag("project-agent-line:$session") and hasAnyAncestor(hasTestTag("project-agent-task:$task")))
                .performClick()
        }
        runOnIdle { assertEquals(listOf("a1", "a2", "b1"), opened) }
    }

    private fun session(id: String, activity: SessionActivity, agent: AgentRuntime = AgentRuntime.Named("Claude")) =
        AgentSessionRow(id, "Session title", agent, activity, null, 0, null, null, false, taskKey = "task")
}
