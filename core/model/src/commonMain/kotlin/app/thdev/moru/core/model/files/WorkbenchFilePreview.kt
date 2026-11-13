package app.thdev.moru.core.model.files

data class WorkbenchFilePreview(
    val path: String,
    val name: String,
    val status: WorkbenchFilePreviewStatus,
    val sizeBytes: Long?,
    val content: String?,
    val summaryText: String,
    val truncated: Boolean = false,
    val truncationText: String? = null,
) {
    companion object {
        fun unavailable(
            path: String,
            message: String,
        ): WorkbenchFilePreview {
            return WorkbenchFilePreview(
                path = path,
                name = path.substringAfterLast('/').ifBlank { path },
                status = WorkbenchFilePreviewStatus.UNAVAILABLE,
                sizeBytes = null,
                content = null,
                summaryText = message,
            )
        }
    }
}
