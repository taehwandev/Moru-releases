package app.thdev.moru.core.domain.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownEditingActionsTest {
    @Test
    fun wrappingAndUnwrappingPreserveSelectionAndSurroundingText() {
        val initial = MarkdownEditValue("a 한글 b", MarkdownTextRange(2, 4))
        val bold = MarkdownEditingActions.wrap(initial, "**")
        assertEquals("a **한글** b", bold.text)
        assertEquals(MarkdownTextRange(4, 6), bold.selection)
        assertEquals(initial, MarkdownEditingActions.wrap(bold, "**"))
    }

    @Test
    fun emptySelectionPlacesCaretInsideMarkers() {
        val value = MarkdownEditingActions.wrap(MarkdownEditValue("", MarkdownTextRange.Zero), "**")
        assertEquals("****", value.text)
        assertEquals(MarkdownTextRange(2), value.selection)
    }

    @Test
    fun linkSelectsItsTargetAndKeepsItsLabel() {
        val value = MarkdownEditingActions.link(MarkdownEditValue("See guide", MarkdownTextRange(4, 9)))
        assertEquals("See [guide](url)", value.text)
        assertEquals("url", value.text.substring(value.selection.min, value.selection.max))
    }

    @Test
    fun checklistFormatsAllSelectedLinesButNotNextLine() {
        val original = MarkdownEditValue("one\ntwo\nthree", MarkdownTextRange(0, 8))
        val task = MarkdownEditingActions.prefix(original, "- [ ] ")
        assertEquals("- [ ] one\n- [ ] two\nthree", task.text)
        assertEquals(original.text, MarkdownEditingActions.prefix(task, "- [ ] ").text)
    }

    @Test
    fun prefixPreservesCrLfAndCaretPosition() {
        val heading = MarkdownEditingActions.prefix(MarkdownEditValue("one\r\ntwo", MarkdownTextRange(7)), "# ")
        assertEquals("one\r\n# two", heading.text)
        assertEquals(MarkdownTextRange(9), heading.selection)
    }
}
