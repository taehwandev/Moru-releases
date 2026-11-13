package app.thdev.moru.feature.sessions.results

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.ConversationTurn
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.terminal.SessionResults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ResultsPageTest {
    private val ids = listOf("done-a", "done-b", "ask", "running")

    @AfterTest
    fun forget() = ids.forEach(SessionResults::forget)

    @Test
    fun onlyOutcomesAreListedAndWhatWaitsOnTheUserComesFirst() {
        val rows = listOf(
            row("running", SessionActivity.Working, at = 50),
            row("done-a", SessionActivity.Quiet, at = 10),
            row("ask", SessionActivity.Quiet, at = 5).copy(replyQuestion = "Should I also update the rail?"),
            row("done-b", SessionActivity.Quiet, at = 30),
            // Already opened: still a result until marked seen; an idle shell with nothing to report is not.
            row("opened", SessionActivity.Waiting, at = 20),
            row("shell", SessionActivity.Idle, at = 40).copy(agent = AgentRuntime.Terminal),
        )
        val items = resultItems(rows, tasks, emptySet(), mapOf("done-a" to "Fixed it.", "opened" to "Viewed.", "shell" to "x"))
        assertEquals(listOf("done-b", "opened", "done-a", "ask"), items.map { it.row.sessionId })
        assertEquals(ResultKind.Question, items.single { it.row.sessionId == "ask" }.kind)
        assertEquals("Fixed it.", items.single { it.row.sessionId == "done-a" }.text)
        // The same session's next result is a different result.
        assertTrue(items[1].seenKey != items[1].copy(text = "Fixed it again.").seenKey)
    }

    @Test
    fun yesAndNoAreOfferedOnlyForAYesOrNoQuestion() {
        assertTrue(isYesNoQuestion("Should I also update the rail?"))
        assertTrue(isYesNoQuestion("왼쪽 레일도 바꿀까요?"))
        assertFalse(isYesNoQuestion("Which color do you prefer?"))
        assertFalse(isYesNoQuestion("어떤 색으로 할까요?"))
    }

    @Test
    fun theCardShowsTheQuestionOnceAndHeadingsAsBoldLines() {
        assertEquals("Cleaned up the sidebar.", "Cleaned up the sidebar.\n\nShould I also update the rail?\n"
            .withoutTrailingQuestion("Should I also update the rail?"))
        assertEquals("Done.", "Done.".withoutTrailingQuestion("Anything else?"))
        assertEquals("**Changes**\n- one", "## Changes\n- one".withSmallHeadings())
    }

    @Test
    fun theLatestResultEndsTheThreadEvenBeforeTheRecordCatchesUp() {
        val thread = listOf(ConversationTurn("Fix the links", "Fixed the preview links."))
        assertEquals(thread, resultTurns(thread, "Fixed the preview links."))
        assertEquals(thread + ConversationTurn(null, "Added a test."), resultTurns(thread, "Added a test."))
        assertEquals(listOf(ConversationTurn(null, "Done.")), resultTurns(emptyList(), "Done."))
        assertEquals("Fixed the links", resultPreview("## **Fixed** the links\n\nmore"))
    }

    @Test
    fun theChosenResultReadsWithItsExchangesAndIsAnsweredOnThePage() = runDesktopComposeUiTest {
        val sent = mutableListOf<Pair<String, String>>()
        SessionResults.registerSender("done-a") { text -> sent += "done-a" to text; true }
        SessionResults.registerSender("ask") { text -> sent += "ask" to text; true }
        SessionResults.recordTranscript("done-a", "**Fixed** the preview links.")
        SessionResults.recordThread("done-a", listOf(
            ConversationTurn("Why do preview links open twice?", "The click handler is bound twice."),
            ConversationTurn("Fix it", "**Fixed** the preview links."),
        ))
        var seen by mutableStateOf(emptySet<Int>())
        val opened = mutableListOf<String>()
        setContent {
            MoruTheme {
                ResultsPage(
                    rows = listOf(
                        row("done-a", SessionActivity.Quiet, at = 10),
                        row("ask", SessionActivity.Quiet, at = 5).copy(replyQuestion = "Should I also update the rail?"),
                    ),
                    tasks = tasks,
                    onOpenSession = { row, _ -> opened += row.sessionId },
                    seen = seen,
                    onSeen = { seen = seen + it },
                )
            }
        }
        // What waits on the user is chosen first and answered right there.
        onNodeWithTag("result-detail:ask").assertExists()
        onNodeWithTag("result-quick-answer:ask:Yes").performClick()
        assertEquals(listOf("ask" to "Yes"), sent)

        onNodeWithTag("result-row:done-a").performClick()
        onNodeWithText("Why do preview links open twice?").assertExists()
        onNodeWithText("The click handler is bound twice.", substring = true).assertExists()
        onNode(hasText("Fixed", substring = true) and hasAnyAncestor(hasTestTag("result-thread:done-a"))).assertExists()
        onNodeWithTag("result-reply:done-a").performTextInput("Now add a test")
        onNodeWithTag("result-send:done-a").performClick()
        assertEquals("done-a" to "Now add a test", sent.last())

        SessionResults.recordChanges("done-a", 3)
        waitForIdle()
        onNodeWithText("3 uncommitted files", ignoreCase = true).assertExists()
        onNodeWithTag("result-open:done-a").performClick()
        assertEquals(listOf("done-a"), opened)
        onNodeWithTag("result-seen:done-a").performClick()
        waitForIdle()
        onNodeWithTag("results-seen").assertExists()
        onAllNodesWithTag("result-seen:done-a").assertCountEquals(0)
    }

    private val tasks = mapOf("t" to SessionTaskRef("Fix preview links", "p1", "Moru"))

    private fun row(id: String, activity: SessionActivity, at: Long) = AgentSessionRow(
        sessionId = id, title = "T$id", agent = AgentRuntime.Named("Claude"), activity = activity,
        lastOutputAtMillis = at * 1000, nowMillis = 100_000, exitCode = null, previewUrl = null, selected = false,
        taskKey = "t",
    )
}
