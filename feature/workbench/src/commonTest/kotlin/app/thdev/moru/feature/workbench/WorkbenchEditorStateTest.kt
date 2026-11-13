package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkbenchEditorStateTest {
    private fun textPreview(
        content: String,
        truncated: Boolean = false,
    ): WorkbenchFilePreview = WorkbenchFilePreview(
        path = "docs/readme.md",
        name = "readme.md",
        status = WorkbenchFilePreviewStatus.TEXT,
        sizeBytes = content.length.toLong(),
        content = content,
        summaryText = "preview",
        truncated = truncated,
        truncationText = if (truncated) "bounded" else null,
    )

    @Test
    fun fullTextPreviewIsEditableAndStartsClean() {
        val state = WorkbenchEditorState.of(textPreview("hello\n"))

        assertTrue(state.canEdit)
        assertFalse(state.isDirty)
        assertEquals("hello\n", state.draft)
    }

    @Test
    fun editingDraftMarksStateDirty() {
        val state = WorkbenchEditorState.of(textPreview("hello\n"))
            .withDraft("hello world\n")

        assertTrue(state.isDirty)
    }

    @Test
    fun savingAdoptsDraftAsBaselineAndClearsDirty() {
        val saved = WorkbenchEditorState.of(textPreview("hello\n"))
            .withDraft("hello world\n")
            .markSaved()

        assertFalse(saved.isDirty)
        assertEquals("hello world\n", saved.baseline)
        assertEquals("hello world\n", saved.draft)
    }

    @Test
    fun truncatedPreviewIsNotEditableAndNeverDirty() {
        val state = WorkbenchEditorState.of(textPreview("partial", truncated = true))
            .withDraft("edited")

        assertFalse(state.canEdit)
        assertFalse(state.isDirty)
    }

    @Test
    fun binaryPreviewIsNotEditable() {
        val binary = WorkbenchFilePreview(
            path = "image.bin",
            name = "image.bin",
            status = WorkbenchFilePreviewStatus.BINARY,
            sizeBytes = 5,
            content = null,
            summaryText = "binary",
        )

        val state = WorkbenchEditorState.of(binary)

        assertFalse(state.canEdit)
        assertFalse(state.isDirty)
    }

    @Test
    fun savingPreservesPreviewMetadataAndThePublicDataClassShape() {
        val preview = textPreview("before").copy(sizeBytes = 123, summaryText = "on disk", truncationText = "metadata")
        val copied = WorkbenchEditorState.of(preview).copy(draft = "after")
        val saved = copied.markSaved()
        val (savedPreview, savedDraft) = saved

        assertEquals(preview.copy(content = "after"), savedPreview)
        assertEquals("after", savedDraft)
        assertEquals("before", copied.preview.content)
        assertTrue(copied.isDirty)
        assertFalse(saved.isDirty)
        val rebound = copied.copy(preview = preview.copy(content = "external", status = WorkbenchFilePreviewStatus.UNAVAILABLE))
        assertEquals("after", rebound.draft)
        assertEquals("external", rebound.baseline)
        assertFalse(rebound.canEdit)
        assertFalse(rebound.isDirty)
    }

    @Test
    fun canonicallyEquivalentTextIsStillAnUnsavedChange() {
        val state = WorkbenchEditorState.of(textPreview("\u00E9")).withDraft("e\u0301")

        assertTrue(state.isDirty)
        assertEquals("\u00E9", state.baseline)
        assertEquals("e\u0301", state.markSaved().baseline)
        assertFalse(state.withDraft("\u00E9").isDirty)
    }

    @Test
    fun missingTextContentStaysReadOnlyUntilTheExistingSaveAcknowledgment() {
        val state = WorkbenchEditorState.of(textPreview("").copy(content = null)).withDraft("saved")

        assertEquals("", state.baseline)
        assertFalse(state.canEdit)
        assertFalse(state.isDirty)
        val saved = state.markSaved()
        assertTrue(saved.canEdit)
        assertFalse(saved.isDirty)
        assertEquals("saved", saved.preview.content)
    }
}
