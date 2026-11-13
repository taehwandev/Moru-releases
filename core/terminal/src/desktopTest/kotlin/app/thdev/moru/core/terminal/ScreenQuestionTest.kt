package app.thdev.moru.core.terminal

import com.jediterm.terminal.model.CharBuffer
import com.jediterm.terminal.model.StyleState
import com.jediterm.terminal.model.TerminalTextBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScreenQuestionTest {
    @Test
    fun claudeAskingToRunACommandIsAQuestion() {
        val screen = listOf(
            "╭──────────────────────────────────────────╮",
            "│ Bash command                             │",
            "│                                          │",
            "│   ./gradlew test                         │",
            "│   Run the tests                          │",
            "│                                          │",
            "│ Do you want to proceed?                  │",
            "│ ❯ 1. Yes                                 │",
            "│   2. Yes, and don't ask again for gradlew│",
            "│   3. No, and tell Claude what to do differently (esc) │",
            "╰──────────────────────────────────────────╯",
        )
        assertEquals("Do you want to proceed?", screenQuestion(screen))
    }

    @Test
    fun codexAndGeminiApprovalsAreQuestions() {
        val codex = listOf(
            "  Would you like to run the following command?",
            "",
            "  $ rm -rf build",
            "",
            "› 1. Yes, proceed (y)",
            "  2. Yes, and don't ask again for this command (p)",
            "  3. No, and tell Codex what to do differently (esc)",
            "",
            "  Press enter to confirm or esc to cancel",
        )
        assertEquals("Would you like to run the following command?", screenQuestion(codex))
        val gemini = listOf(
            "Allow execution of: 'npm install'?",
            "● 1. Allow once",
            "  2. Allow for this session",
            "  3. No, suggest changes (esc)",
        )
        assertEquals("Allow execution of: 'npm install'?", screenQuestion(gemini))
    }

    @Test
    fun aYesNoPromptOnTheLastLineIsAQuestion() {
        assertEquals("Overwrite config.json? (y/N)", screenQuestion(listOf("$ ./setup", "Overwrite config.json? (y/N)")))
        assertEquals("Continue? [Y/n]", screenQuestion(listOf("Continue? [Y/n]", "")))
    }

    @Test
    fun ordinaryOutputAndTheAgentsOwnInputBoxAreNotQuestions() {
        assertNull(screenQuestion(listOf("Reading the project", "› ", "? for shortcuts  100% context left")))
        assertNull(screenQuestion(listOf("What should I fix?", "> 1. the login bug")))
        assertNull(screenQuestion(listOf("Plan:", "1. Read the code", "2. Fix it", "> ")))
        assertNull(screenQuestion(listOf("Was (y/n) answered?", "Done.")))
        assertNull(screenQuestion(emptyList()))
    }

    @Test
    fun aQuestionScrolledFarAboveTheChoicesIsNotTheOneAsked() {
        val screen = listOf("Do you want to proceed?") + List(30) { "line $it" } + listOf("❯ 1. Yes", "2. No")
        assertNull(screenQuestion(screen))
    }

    @Test
    fun theEmulatorScreenIsReadRowByRow() {
        val buffer = TerminalTextBuffer(80, 8, StyleState(), 0)
        buffer.writeString(0, 2, CharBuffer(" Do you want to make this edit to Main.kt?"))
        buffer.writeString(0, 3, CharBuffer(" ❯ 1. Yes"))
        buffer.writeString(0, 4, CharBuffer("   2. No, and tell Claude what to do differently (esc)"))
        buffer.lock()
        try {
            assertEquals("Do you want to make this edit to Main.kt?", screenQuestion(terminalScreenRows(buffer)))
        } finally {
            buffer.unlock()
        }
    }
}
