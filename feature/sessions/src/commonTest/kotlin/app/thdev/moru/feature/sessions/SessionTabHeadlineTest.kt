package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionTabHeadlineTest {
    private fun row(summary: String?, title: String = "", activity: SessionActivity = SessionActivity.Quiet, lastLine: String? = null) =
        AgentSessionRow(
            sessionId = "1", title = title, agent = AgentRuntime.Terminal, activity = activity, lastOutputAtMillis = null,
            nowMillis = 0, exitCode = null, previewUrl = null, selected = false, lastLine = lastLine, summary = summary,
        )

    @Test
    fun theProjectNameCodexAppendsIsLeftOut() {
        assertEquals("상태 색상 구분 개선", sessionTabHeadline(row("상태 색상 구분 개선 ¦ Moru"), "Codex"))
    }

    @Test
    fun aSummaryWithoutAProjectIsKeptWhole() {
        assertEquals("Fix the sidebar", sessionTabHeadline(row("Fix the sidebar"), "Claude"))
    }

    @Test
    fun withoutASummaryTheWorkingLineThenTheTitleThenTheAgentLead() {
        assertEquals("compiling", sessionTabHeadline(row(null, "T", SessionActivity.Working, "compiling"), "Claude"))
        assertEquals("Claude · resumed", sessionTabHeadline(row(null, "Claude · resumed"), "Claude"))
        assertEquals("Claude", sessionTabHeadline(row(" ¦ Moru"), "Claude"))
    }

    @Test
    fun aQuestionOrOutputCanRepeatTheHeadlineWithSurroundingWhitespace() {
        assertTrue(sessionTextMatches("Allow this command?", "Allow this command?"))
        assertTrue(sessionTextMatches("\n Allow this command?\t", "Allow this command?"))
        assertTrue(sessionTextMatches("Checking 12 files", " Checking 12 files "))
    }

    @Test
    fun missingTextAndDifferentQuestionsOrCommandsAreNotDuplicates() {
        assertFalse(sessionTextMatches(null, "Review changes"))
        assertFalse(sessionTextMatches("  ", ""))
        assertFalse(sessionTextMatches("Allow this command?", "Review changes"))
        assertFalse(sessionTextMatches("git push origin main", "git push origin preview"))
        assertFalse(sessionTextMatches("Checking Files", "Checking files"))
        assertFalse(sessionTextMatches("echo one\necho two", "echo one echo two"))
        assertFalse(sessionTextMatches("echo 'two  spaces'", "echo 'two spaces'"))
    }
}
