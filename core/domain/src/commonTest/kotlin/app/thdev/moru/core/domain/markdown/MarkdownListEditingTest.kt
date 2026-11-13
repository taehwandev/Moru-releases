package app.thdev.moru.core.domain.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MarkdownListEditingTest {
    @Test
    fun enterContinuesEachBulletAndLeavesFollowingLinesAlone() {
        for (bullet in listOf("-", "+", "*")) {
            val original = "$bullet first\nlast"
            val result = enter(original, original.indexOf('\n'))
            assertEquals("$bullet first\n$bullet \nlast", result.text)
            assertEquals(MarkdownTextRange(original.indexOf('\n') + 3), result.selection)
        }
    }

    @Test
    fun enterSplitsAtCaretAndPreservesUnicodeAndTrailingContent() {
        val result = enter("  - 한글🙂 tail\nnext", 6)
        assertEquals("  - 한글\n  - 🙂 tail\nnext", result.text)
        assertEquals(MarkdownTextRange(11), result.selection)
    }

    @Test
    fun enterIncrementsNumbersAndRetainsDelimiterPaddingAndSpacing() {
        assertEquals("9) nine\n10) ", enter("9) nine").text)
        assertEquals("009.  nine\n010.  ", enter("009.  nine").text)
        assertEquals("  42. item\n  43. ", enter("  42. item").text)
        assertNull(MarkdownListEditing.enter(value("999999999. end")))
    }

    @Test
    fun enterContinuesCheckedAndUncheckedTasksAsUnchecked() {
        for (check in listOf(" ", "x", "X")) {
            assertEquals("- [$check] done\n- [ ] ", enter("- [$check] done").text)
        }
        assertEquals("3) [x] done\n4) [ ] ", enter("3) [x] done").text)
    }

    @Test
    fun emptyItemsExitOneLevelWithoutAddingNewline() {
        for (source in listOf("- ", "+   ", "12. ", "- [ ] ", "- [x]")) {
            assertEquals(MarkdownEditValue("", MarkdownTextRange.Zero), enter(source))
        }
        assertEquals("  - ", enter("    - ").text)
        assertEquals("- [ ] ", enter("  - [ ] ").text)
        assertEquals("\t- ", enter("\t\t- ").text)
        assertEquals("first\n\nlast", enter("first\n- \nlast", 8).text)
    }

    @Test
    fun enterDoesNotHandleSelectionCompositionOrCaretInsideMarker() {
        assertNull(MarkdownListEditing.enter(MarkdownEditValue("- item", MarkdownTextRange(2, 4))))
        assertNull(MarkdownListEditing.enter(MarkdownEditValue("- 한글", MarkdownTextRange(4), MarkdownTextRange(2, 4))))
        assertNull(MarkdownListEditing.enter(value("- item", 1)))
        assertNull(MarkdownListEditing.enter(value("- [x] item", 4)))
        assertEquals("- \n- item", enter("- item", 2).text)
    }

    @Test
    fun enterPreservesCrLfForCurrentAndFinalLines() {
        assertEquals("- first\r\n- \r\nlast", enter("- first\r\nlast", 7).text)
        assertEquals("first\r\n- last\r\n- ", enter("first\r\n- last").text)
        assertNull(MarkdownListEditing.enter(value("- item\r\nnext", 7)))
    }

    @Test
    fun tabMovesCurrentItemAndCaret() {
        val indented = indent("- 한글", MarkdownTextRange(4))
        assertEquals("  - 한글", indented.text)
        assertEquals(MarkdownTextRange(6), indented.selection)
        assertEquals(value("- 한글", 4), MarkdownListEditing.indent(indented, outdent = true))
        assertNull(MarkdownListEditing.indent(value("- item"), outdent = true))
    }

    @Test
    fun selectedItemsKeepRelativeNestingAndSelectionDirection() {
        val text = "- one\n  - two\nlast"
        val selection = MarkdownTextRange(text.indexOf("last"), 2)
        val indented = indent(text, selection)
        assertEquals("  - one\n    - two\nlast", indented.text)
        assertEquals(MarkdownTextRange(selection.start + 4, selection.end + 2), indented.selection)
        assertEquals(MarkdownEditValue(text, selection), MarkdownListEditing.indent(indented, outdent = true))
    }

    @Test
    fun tabPreservesCrLfAndDoesNotIncludeLineAtSelectionEnd() {
        val text = "- one\r\n- two\r\nplain"
        val result = indent(text, MarkdownTextRange(0, text.indexOf("plain")))
        assertEquals("  - one\r\n  - two\r\nplain", result.text)
        assertEquals(MarkdownTextRange(2, text.indexOf("plain") + 4), result.selection)
    }

    @Test
    fun outdentUsesSharedIndentToPreserveRelativeDepth() {
        val text = " - parent\n  - child"
        assertEquals("- parent\n - child", indent(text, MarkdownTextRange(0, text.length), outdent = true).text)
        val tabs = "\t- parent\n\t\t- child"
        val indented = indent(tabs, MarkdownTextRange(0, tabs.length))
        assertEquals("\t\t- parent\n\t\t\t- child", indented.text)
        assertEquals(MarkdownEditValue(tabs, MarkdownTextRange(0, tabs.length)), MarkdownListEditing.indent(indented, outdent = true))
        assertNull(MarkdownListEditing.indent(MarkdownEditValue("- a\n  - b", MarkdownTextRange(0, 9)), outdent = true))
    }

    @Test
    fun ordinaryTextMixedSelectionsAndImeAreLeftToTextField() {
        for (text in listOf("plain", "---", "- - -", "* * *", "1.item", "[ ] task")) {
            assertNull(MarkdownListEditing.enter(value(text)))
            assertNull(MarkdownListEditing.indent(value(text), outdent = false))
        }
        assertNull(MarkdownListEditing.indent(MarkdownEditValue("- item\nplain", MarkdownTextRange(0, 12)), outdent = false))
        assertNull(MarkdownListEditing.indent(MarkdownEditValue("- 한글", MarkdownTextRange(4), MarkdownTextRange(2, 4)), outdent = false))
    }

    @Test
    fun fencedCodeAndFenceLinesAreNeverEdited() {
        for (fence in listOf("```kotlin", "~~~~ markdown", "  ``` kotlin", "- ```kotlin", "- [ ] ```kotlin")) {
            val marker = if (fence.contains('~')) "~~~~~" else "````"
            val text = "$fence\n- code\n$marker  \n- outside"
            assertNull(MarkdownListEditing.enter(value(text, text.indexOf("- code") + 6)))
            assertNull(MarkdownListEditing.indent(value(text, text.indexOf("- code") + 6), outdent = false))
            assertNull(MarkdownListEditing.enter(value(text, fence.length)))
            assertEquals("$text\n- ", enter(text).text)
        }
    }

    @Test
    fun mismatchedOrShortClosingFencesDoNotEndCode() {
        for (closing in listOf("~~~", "``", "``` not a close", "- ````", "- [ ] ````")) {
            val text = "```` language\n$closing\n- still code"
            assertNull(MarkdownListEditing.enter(value(text)))
            assertNull(MarkdownListEditing.indent(value(text), outdent = false))
        }
        val selection = MarkdownEditValue("- before\n```\n- code\n```\n- after", MarkdownTextRange(0, 34))
        assertNull(MarkdownListEditing.indent(selection, outdent = false))
    }

    private fun value(text: String, caret: Int = text.length) = MarkdownEditValue(text, MarkdownTextRange(caret))
    private fun enter(text: String, caret: Int = text.length) = assertNotNull(MarkdownListEditing.enter(value(text, caret)))
    private fun indent(text: String, selection: MarkdownTextRange, outdent: Boolean = false) =
        assertNotNull(MarkdownListEditing.indent(MarkdownEditValue(text, selection), outdent))
}
