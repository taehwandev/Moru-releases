package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownEditingActionsTest {
    @Test
    fun wrappingAndUnwrappingPreserveSelectionAndSurroundingText() {
        val initial = TextFieldValue("a 한글 b", TextRange(2, 4))
        val bold = MarkdownEditingActions.wrap(initial, "**")
        assertEquals("a **한글** b", bold.text)
        assertEquals(TextRange(4, 6), bold.selection)
        assertEquals(initial, MarkdownEditingActions.wrap(bold, "**"))
    }

    @Test
    fun emptySelectionPlacesCaretInsideMarkers() {
        val value = MarkdownEditingActions.wrap(TextFieldValue("", TextRange.Zero), "**")
        assertEquals("****", value.text)
        assertEquals(TextRange(2), value.selection)
    }

    @Test
    fun linkSelectsItsTargetAndKeepsItsLabel() {
        val value = MarkdownEditingActions.link(TextFieldValue("See guide", TextRange(4, 9)))
        assertEquals("See [guide](url)", value.text)
        assertEquals("url", value.text.substring(value.selection.min, value.selection.max))
    }

    @Test
    fun checklistFormatsAllSelectedLinesButNotNextLine() {
        val original = TextFieldValue("one\ntwo\nthree", TextRange(0, 8))
        val task = MarkdownEditingActions.prefix(original, "- [ ] ")
        assertEquals("- [ ] one\n- [ ] two\nthree", task.text)
        assertEquals(original.text, MarkdownEditingActions.prefix(task, "- [ ] ").text)
    }

    @Test
    fun prefixPreservesCrLfAndCaretPosition() {
        val heading = MarkdownEditingActions.prefix(TextFieldValue("one\r\ntwo", TextRange(7)), "# ")
        assertEquals("one\r\n# two", heading.text)
        assertEquals(TextRange(9), heading.selection)
    }
}
