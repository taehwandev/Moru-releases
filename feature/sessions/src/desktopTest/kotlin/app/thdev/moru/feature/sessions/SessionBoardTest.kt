package app.thdev.moru.feature.sessions

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.terminal.SessionPermissionChoice
import app.thdev.moru.core.terminal.SessionPermissionPrompt
import app.thdev.moru.core.terminal.SessionPermissions
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SessionBoardTest {
    @Test
    fun everyActivityHasOneGroupAndOnlyAnAgentTurnIsCompleted() {
        val expected = mapOf(
            SessionActivity.Starting to SessionBoardSection.Running,
            SessionActivity.Working to SessionBoardSection.Running,
            SessionActivity.Asking to SessionBoardSection.NeedsAttention,
            SessionActivity.Failed to SessionBoardSection.NeedsAttention,
            SessionActivity.Quiet to SessionBoardSection.Completed,
            SessionActivity.Waiting to SessionBoardSection.Ready,
            SessionActivity.Idle to SessionBoardSection.Ready,
            SessionActivity.Exited to SessionBoardSection.Ready,
        )
        assertEquals(SessionActivity.entries.toSet(), expected.keys)
        expected.forEach { (activity, section) -> assertEquals(section, row("1", activity).boardSection(), "$activity") }
        // A quiet plain terminal, such as a server, still runs its program.
        assertEquals(SessionBoardSection.Running, row("1", SessionActivity.Quiet, AgentRuntime.Terminal).boardSection())
        assertEquals(SessionBoardSection.NeedsAttention, row("1", SessionActivity.Working).boardSection(hasPermissionRequest = true))
        // A finished turn that ended with a question waits for the user's reply; while working, its question is not yet.
        val asked = row("1", SessionActivity.Quiet).copy(replyQuestion = "Which color do you prefer?")
        assertEquals(SessionBoardSection.AwaitingReply, asked.boardSection())
        assertEquals(SessionBoardSection.Running, asked.copy(activity = SessionActivity.Working).boardSection())
    }

    @Test
    fun stableAttentionOrderingLeadsWithQuestionsThenFailures() {
        val ordered = listOf(
            row("1", SessionActivity.Idle), row("2", SessionActivity.Quiet), row("3", SessionActivity.Failed),
            row("4", SessionActivity.Asking), row("5", SessionActivity.Exited),
        ).byBoardUrgency().map { it.sessionId }
        assertEquals(listOf("4", "3", "2", "1", "5"), ordered)
    }

    @Test
    fun everySessionIsListedWithItsTaskAndWhatItIsAboutAndOpensOnPress() = runDesktopComposeUiTest {
        val picked = mutableListOf<Pair<String, String?>>()
        setContent {
            MoruTheme {
                SessionBoard(
                    rows = listOf(
                        row("1", SessionActivity.Working, task = "a", summary = "Splitting the sidebar views"),
                        row("2", SessionActivity.Asking, task = "b"),
                        row("3", SessionActivity.Idle, task = "a", title = "Shell in login"),
                        row("4", SessionActivity.Quiet, task = "a"),
                        row("5", SessionActivity.Exited, task = "b"),
                    ),
                    tasks = tasks,
                    onSelectSession = { row, project -> picked += row.sessionId to project },
                    onOpenPreview = { _, _ -> },
                )
            }
        }
        onNodeWithText("Running").assertExists()
        onNodeWithText("Needs attention").assertExists()
        onNodeWithText("Response ready").assertExists()
        onNodeWithText("Waiting").assertExists()
        onNodeWithText("Splitting the sidebar views").assertExists()
        // A session at its prompt is listed too, not folded away.
        onNodeWithText("Shell in login").assertExists()
        (1..5).forEach { onNodeWithTag("session-row:$it").assertExists() }
        onNode(hasTestTag("session-row:1") and hasAnyAncestor(hasTestTag("session-board-project:p1")) and
            hasAnyAncestor(hasTestTag("session-board-task:a"))).assertExists()
        onNodeWithText("Alpha · Fix login").assertDoesNotExist()
        onNodeWithTag("session-row:2").performScrollTo().performClick()
        runOnIdle { assertEquals(listOf<Pair<String, String?>>("2" to "p2"), picked) }
    }

    @Test
    fun emptySectionsAreHiddenWhenAnySessionsExist() = runDesktopComposeUiTest {
        setContent {
            MoruTheme {
                SessionBoard(listOf(row("1", SessionActivity.Working, task = "a")), tasks, { _, _ -> }, { _, _ -> })
            }
        }
        onNodeWithTag("session-board-section:Running").assertExists()
        listOf("NeedsAttention", "Completed", "Ready").forEach {
            onNodeWithTag("session-board-section:$it").assertDoesNotExist()
        }
        onNodeWithText("No sessions are running.").assertDoesNotExist()
    }

    @Test
    fun anEmptyBoardHasOneHonestEmptyState() = runDesktopComposeUiTest {
        setContent { MoruTheme { SessionBoard(emptyList(), tasks, { _, _ -> }, { _, _ -> }) } }
        onNodeWithText("No sessions are running.").assertExists()
        SessionBoardSection.entries.forEach { onNodeWithTag("session-board-section:$it").assertDoesNotExist() }
    }

    @Test
    fun duplicateProjectAndTaskTitlesKeepTheirOwnershipAndMissingReferencesRemainReachable() = runDesktopComposeUiTest {
        val picked = mutableListOf<Pair<String, String?>>()
        val duplicateTasks = mapOf(
            "a" to SessionTaskRef("Same task", "p1", "Same project"),
            "b" to SessionTaskRef("Same task", "p2", "Same project"),
            "c" to SessionTaskRef("Same task", "p1", "Same project"),
        )
        setContent {
            MoruTheme {
                SessionBoard(listOf(row("a", SessionActivity.Working, task = "a"),
                    row("b", SessionActivity.Working, task = "b"), row("c", SessionActivity.Working, task = "c"),
                    row("missing", SessionActivity.Working, task = "deleted"), row("unattached", SessionActivity.Working)),
                    duplicateTasks, { row, project -> picked += row.sessionId to project }, { _, _ -> })
            }
        }
        onAllNodesWithText("Same project").assertCountEquals(2)
        onAllNodesWithText("Same task").assertCountEquals(3)
        listOf("a" to "p1", "b" to "p2", "c" to "p1", "missing" to "unattached", "unattached" to "unattached")
            .forEach { (session, project) ->
                onNode(hasTestTag("session-row:$session") and
                    hasAnyAncestor(hasTestTag("session-board-project:$project")) and
                    hasAnyAncestor(hasTestTag("session-board-task:${if (project == "unattached") "unattached" else session}")))
                    .performScrollTo().performClick()
            }
        onNodeWithText("Sessions without a Task").assertExists()
        runOnIdle { assertEquals(listOf("a" to "p1", "b" to "p2", "c" to "p1", "missing" to null, "unattached" to null), picked) }
    }

    @Test
    fun livePermissionsMoveTheOwningSessionAndKeepPermissionAndPreviewActions() = runDesktopComposeUiTest {
        val session = "live-board-permission"
        val picked = mutableListOf<Pair<String, String?>>()
        val previews = mutableListOf<Pair<String, String>>()
        var choice: String? = null
        try {
            setContent {
                MoruTheme {
                    SessionBoard(listOf(row(session, SessionActivity.Working, task = "b").copy(previewUrl = "http://localhost:5173")),
                        tasks, { row, project -> picked += row.sessionId to project }, { id, url -> previews += id to url })
                }
            }
            runOnIdle {
                SessionPermissions.observe(session, SessionPermissionPrompt("Allow command?", "pwd",
                    listOf(SessionPermissionChoice("allow", "Allow once")))) { choice = it; true }
            }
            onNode(hasTestTag("session-row:$session") and
                hasAnyAncestor(hasTestTag("session-board-section:NeedsAttention")) and
                hasAnyAncestor(hasTestTag("session-board-project:p2")) and
                hasAnyAncestor(hasTestTag("session-board-task:b"))).assertExists()
            onNodeWithTag("session-board-section:Running").assertDoesNotExist()
            onNodeWithText("Preview localhost:5173").performClick()
            onNodeWithTag("permission-choice-$session-allow").performClick()
            onNode(hasTestTag("session-row:$session") and
                hasAnyAncestor(hasTestTag("session-board-section:Running"))).performClick()
            runOnIdle {
                assertEquals("allow", choice)
                assertEquals(listOf<Pair<String, String?>>(session to "p2"), picked)
                assertEquals(listOf(session to "http://localhost:5173"), previews)
            }
        } finally { SessionPermissions.forget(session) }
    }

    private fun row(
        id: String,
        activity: SessionActivity,
        agent: AgentRuntime = AgentRuntime.Named("Claude"),
        task: String? = null,
        summary: String? = null,
        title: String = "T$id",
    ) = AgentSessionRow(
        sessionId = id, title = title, agent = agent, activity = activity, lastOutputAtMillis = null, nowMillis = 0,
        exitCode = null, previewUrl = null, selected = false, taskKey = task, summary = summary,
    )

    private val tasks = mapOf(
        "a" to SessionTaskRef("Fix login", "p1", "Alpha"),
        "b" to SessionTaskRef("Add billing", "p2", "Beta"),
    )
}
