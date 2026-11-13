package app.thdev.moru.core.domain.files

import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus

/** Pure text draft state; callers own file reads and invoke [markSaved] only after a successful write. */
data class FileEditorState(
    val status: WorkbenchFilePreviewStatus,
    val content: String?,
    val truncated: Boolean,
    val draft: String = content.orEmpty(),
) {
    val baseline: String get() = content.orEmpty()
    val canEdit: Boolean get() = status == WorkbenchFilePreviewStatus.TEXT && !truncated && content != null
    // Kotlin String equality compares UTF-16 content, without canonical Unicode normalization.
    val isDirty: Boolean get() = canEdit && draft != baseline

    fun withDraft(nextDraft: String): FileEditorState = copy(draft = nextDraft)
    fun markSaved(): FileEditorState = copy(content = draft)
}
