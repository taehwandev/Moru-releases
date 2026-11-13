package app.thdev.moru.feature.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class CopiedTerminalTextTest {
    private val width = 40

    @Test
    fun theSpacesAProgramPaddedEachLineWithAreDropped() {
        assertEquals("ls -la\npwd", tidyCopiedTerminalText("ls -la" + " ".repeat(34) + "\npwd   ", width))
    }

    @Test
    fun aCommandTheAgentWrappedAndIndentedPastesAsOneLine() {
        // "⏺ Bash(" style: the CLI broke between words near the edge and indented the rest under the text.
        val first = "  Bash(git -C /Users/me/proj log --one"
        val copied = first.padEnd(width) + "\n      line -3)"
        assertEquals("  Bash(git -C /Users/me/proj log --one line -3)", tidyCopiedTerminalText(copied, width))
    }

    @Test
    fun aWordCutAtTheVeryEdgeContinuesWithoutASpace() {
        val first = "  cd /Users/me/git/some/really/long/path"
        assertEquals(width, first.length)
        assertEquals("  cd /Users/me/git/some/really/long/path/to/repo", tidyCopiedTerminalText("$first\n    /to/repo", width))
    }

    @Test
    fun anIndentedLineThatWouldHaveFitAboveIsAPurposefulBreakAndStays() {
        val code = "def main():\n    print(\"hi\")\n    return 0"
        assertEquals(code, tidyCopiedTerminalText(code, width))
    }

    @Test
    fun aNewUnindentedLineAfterALongOneIsNeverJoined() {
        val copied = "x".repeat(39) + "\n\$ ls"
        assertEquals(copied, tidyCopiedTerminalText(copied, width))
    }

    @Test
    fun aSingleLineOnlyLosesItsPadding() {
        assertEquals("echo hi", tidyCopiedTerminalText("echo hi     ", width))
    }
}
