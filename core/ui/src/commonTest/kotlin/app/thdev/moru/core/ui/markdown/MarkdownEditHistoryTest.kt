package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownEditHistoryTest {
    @Test
    fun undoRedoRestoreTextAndSelectionAndNewEditsDiscardRedo() {
        val initial = TextFieldValue("hello", TextRange(0, 5))
        val history = MarkdownEditHistory(initial)
        val bold = MarkdownEditingActions.wrap(initial, "**")
        history.accept(bold)
        history.accept(bold.copy(selection = TextRange(4)))
        assertEquals(initial, history.undo())
        assertEquals(bold.text, history.redo().text)
        history.undo()
        val replacement = TextFieldValue("new", TextRange(3))
        history.accept(replacement)
        assertEquals(replacement, history.redo())
    }

    @Test
    fun imeCompositionIsOneUndoUnit() {
        val history = MarkdownEditHistory(TextFieldValue(""))
        history.accept(TextFieldValue("ㅎ", TextRange(1), TextRange(0, 1)))
        history.accept(TextFieldValue("하", TextRange(1), TextRange(0, 1)))
        history.accept(TextFieldValue("한", TextRange(1)))
        assertEquals("", history.undo().text)
        assertEquals("한", history.redo().text)
    }

    @Test
    fun historyHasABoundedSnapshotCount() {
        val history = MarkdownEditHistory(TextFieldValue(""))
        repeat(150) { history.accept(TextFieldValue("edit $it")) }
        repeat(200) { history.undo() }
        assertEquals("edit 49", history.value.text)
    }
}
