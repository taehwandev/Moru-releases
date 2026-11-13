package app.thdev.moru.core.domain.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownEditHistoryTest {
    @Test
    fun undoRedoRestoreTextAndSelectionAndNewEditsDiscardRedo() {
        val initial = MarkdownEditValue("hello", MarkdownTextRange(0, 5))
        val history = MarkdownEditHistory(initial)
        val bold = MarkdownEditingActions.wrap(initial, "**")
        history.accept(bold)
        history.accept(bold.copy(selection = MarkdownTextRange(4)))
        assertEquals(initial, history.undo())
        assertEquals(bold.text, history.redo().text)
        history.undo()
        val replacement = MarkdownEditValue("new", MarkdownTextRange(3))
        history.accept(replacement)
        assertEquals(replacement, history.redo())
    }

    @Test
    fun imeCompositionIsOneUndoUnit() {
        val history = MarkdownEditHistory(MarkdownEditValue(""))
        history.accept(MarkdownEditValue("ㅎ", MarkdownTextRange(1), MarkdownTextRange(0, 1)))
        history.accept(MarkdownEditValue("하", MarkdownTextRange(1), MarkdownTextRange(0, 1)))
        history.accept(MarkdownEditValue("한", MarkdownTextRange(1)))
        assertEquals("", history.undo().text)
        assertEquals("한", history.redo().text)
    }

    @Test
    fun historyHasABoundedSnapshotCount() {
        val history = MarkdownEditHistory(MarkdownEditValue(""))
        repeat(150) { history.accept(MarkdownEditValue("edit $it")) }
        repeat(200) { history.undo() }
        assertEquals("edit 49", history.value.text)
    }
}
