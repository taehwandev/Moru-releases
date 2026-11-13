package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.files.FileEditorState
import app.thdev.moru.core.model.files.WorkbenchFilePreview

/**
 * Editor state for the file opened in the Workbench center pane.
 *
 * The [preview] holds the on-disk baseline the file was opened with, and
 * [draft] holds the in-progress edit. Editing is only allowed when the whole
 * file was loaded as text; a truncated, oversized, binary, or missing preview
 * must stay read-only so a save can never overwrite content the editor never
 * saw.
 */
data class WorkbenchEditorState(
    val preview: WorkbenchFilePreview,
    val draft: String,
) {
    private val state = FileEditorState(preview.status, preview.content, preview.truncated, draft)

    /** The content the draft is compared against; empty when nothing was loaded. */
    val baseline: String
        get() = state.baseline

    /** Editable only when the full file content is present and untruncated. */
    val canEdit: Boolean
        get() = state.canEdit

    /** True when an editable draft differs from the saved baseline. */
    val isDirty: Boolean
        get() = state.isDirty

    /** Replace the in-progress draft. */
    fun withDraft(nextDraft: String): WorkbenchEditorState = state.withDraft(nextDraft).let { copy(draft = it.draft) }

    /**
     * Adopt the current draft as the new saved baseline after a successful
     * write, which clears [isDirty] without reopening the file.
     */
    fun markSaved(): WorkbenchEditorState = state.markSaved().let { copy(preview = preview.copy(content = it.content), draft = it.draft) }

    companion object {
        fun of(preview: WorkbenchFilePreview): WorkbenchEditorState =
            WorkbenchEditorState(preview = preview, draft = FileEditorState(preview.status, preview.content, preview.truncated).draft)
    }
}
