package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Screens captured from Claude Code 2.1 and Codex 0.163 after a turn that ended with a question. */
class ScreenReplyQuestionTest {
    private val claude = listOf(
        " ▐▛███▛█   Claude Code v2.1.302",
        "",
        "❯ Do not use any tools. Reply with one short sentence that ends by asking me which color I prefer,",
        "  then stop.",
        "",
        "⏺ 어떤 색을 가장 좋아하세요?",
        "",
        "✻ Churned for 2s · done 1:57 PM",
        "",
        "────────────────────────────────────────",
        "❯",
        "────────────────────────────────────────",
        "  Opus 5.5 · /private/tmp/…/reply-probe · 5h █████▌░░  69%",
        "  ⏵⏵ auto mode on (shift+tab to cycle)",
    )

    private val codex = listOf(
        "  >_ OpenAI Codex (v0.163.0)",
        "› Do not use any tools. Reply with one short sentence that ends by asking me which color I prefer,",
        "  then stop.",
        "",
        "• Which color do you prefer?",
        "",
        "  Worked for 3s • 1:59 PM",
        "",
        "› Ask Codex to do anything",
        "",
        "  GPT-6.1-Sol xhigh fast · /private/tmp/moru-ios-tools/reply-probe · weekly 67% left",
        "  ← for agents · ? for shortcuts",
    )

    @Test
    fun aTurnThatEndsWithAQuestionWaitsForTheReply() {
        assertEquals("어떤 색을 가장 좋아하세요?", screenReplyQuestion(claude))
        assertEquals("Which color do you prefer?", screenReplyQuestion(codex))
    }

    @Test
    fun theQuestionIsTheLastLineOfAMessageOverSeveralLines() {
        val screen = listOf(
            "⏺ I fixed the login redirect and the tests pass.",
            "",
            "  Should I also update the docs?",
            "",
            "✻ Worked for 40s",
            "❯",
        )
        assertEquals("Should I also update the docs?", screenReplyQuestion(screen))
    }

    @Test
    fun aReportOrAToolResultIsNotAQuestion() {
        assertNull(screenReplyQuestion(claude.map { it.replace("어떤 색을 가장 좋아하세요?", "색을 바꿨습니다.") }))
        assertNull(screenReplyQuestion(listOf("⏺ Bash(ls)", "  ⎿  README.md", "", "❯")))
        assertNull(screenReplyQuestion(listOf("$ ls", "README.md", "$ ")))
        assertNull(screenReplyQuestion(emptyList()))
    }

    @Test
    fun theUsersOwnQuestionAboveTheAnswerIsNotTheAgents() {
        val screen = listOf("❯ What does this function do?", "", "⏺ It parses the config file.", "", "❯")
        assertNull(screenReplyQuestion(screen))
    }
}
