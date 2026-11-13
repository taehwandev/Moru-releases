package app.thdev.moru.core.domain.files

import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileEditorStateTest {
    @Test fun completeTextIncludingAnEmptyFileStartsEditableAndClean() {
        for (content in listOf("", "hello\n")) {
            val state = FileEditorState(WorkbenchFilePreviewStatus.TEXT, content, false)
            assertEquals(content, state.content)
            assertEquals(content, state.baseline)
            assertEquals(content, state.draft)
            assertTrue(state.canEdit)
            assertFalse(state.isDirty)
        }
    }

    @Test fun onlyCompleteTextIsEditableAcrossAllStatusesAndMissingContent() {
        val editable = mutableListOf<Pair<WorkbenchFilePreviewStatus, String?>>()
        for (status in WorkbenchFilePreviewStatus.entries) for (truncated in listOf(false, true)) {
            for (content in listOf(null, "", "text")) {
                val state = FileEditorState(status, content, truncated)
                assertEquals(content.orEmpty(), state.baseline)
                assertFalse(state.isDirty)
                if (state.canEdit) editable += status to content
                else assertFalse(state.withDraft("changed").isDirty)
            }
        }
        assertEquals(listOf<Pair<WorkbenchFilePreviewStatus, String?>>(
            WorkbenchFilePreviewStatus.TEXT to "", WorkbenchFilePreviewStatus.TEXT to "text"), editable)
    }

    @Test fun draftReplacementAndRevertingDoNotMutateTheBaseline() {
        val original = FileEditorState(WorkbenchFilePreviewStatus.TEXT, "before", false)
        val edited = original.withDraft("after 😀\n")
        assertTrue(edited.isDirty)
        assertEquals("before", edited.baseline)
        assertEquals("before", original.draft)
        assertFalse(edited.withDraft("before").isDirty)
        assertEquals("explicit", FileEditorState(WorkbenchFilePreviewStatus.TEXT, null, false, "explicit").draft)
    }

    @Test fun savingAdoptsTheDraftAndKeepsTheOtherFields() {
        val edited = FileEditorState(WorkbenchFilePreviewStatus.TEXT, "before", false).withDraft("after\n")
        val saved = edited.markSaved()
        assertEquals(FileEditorState(WorkbenchFilePreviewStatus.TEXT, "after\n", false, "after\n"), saved)
        assertEquals("after\n", saved.baseline)
        assertFalse(saved.isDirty)
        assertTrue(edited.isDirty)
        assertEquals("before", edited.content)
    }

    @Test fun canonicallyEquivalentUnicodeStillDiffersByUtf16Content() {
        for ((composed, decomposed) in listOf("\u00E9" to "e\u0301", "\u00C5" to "A\u030A", "\uAC00" to "\u1100\u1161")) {
            val edited = FileEditorState(WorkbenchFilePreviewStatus.TEXT, composed, false).withDraft(decomposed)
            assertTrue(edited.isDirty)
            assertEquals(composed, edited.baseline)
            val saved = edited.markSaved()
            assertEquals(decomposed, saved.baseline)
            assertFalse(saved.isDirty)
        }
    }

    @Test fun lineEndingsAndSupplementaryCharactersAreComparedExactly() {
        val state = FileEditorState(WorkbenchFilePreviewStatus.TEXT, "😀\n", false)
        assertFalse(state.withDraft(buildString { append("😀"); append('\n') }).isDirty)
        assertTrue(state.withDraft("😀\r\n").isDirty)
        assertTrue(state.withDraft("😁\n").isDirty)
        assertTrue(state.withDraft("").isDirty)
    }

    @Test fun markSavedRetainsTheOriginalUnconditionalTransition() {
        for (status in WorkbenchFilePreviewStatus.entries) {
            val saved = FileEditorState(status, null, true, "replacement").markSaved()
            assertEquals(status, saved.status)
            assertTrue(saved.truncated)
            assertEquals("replacement", saved.content)
            assertFalse(saved.canEdit)
            assertFalse(saved.isDirty)
        }
        val completed = FileEditorState(WorkbenchFilePreviewStatus.TEXT, null, false, "saved").markSaved()
        assertTrue(completed.canEdit)
        assertFalse(completed.isDirty)
    }
}
