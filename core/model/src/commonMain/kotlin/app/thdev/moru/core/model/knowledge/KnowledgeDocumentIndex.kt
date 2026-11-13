package app.thdev.moru.core.model.knowledge

/** Relative Markdown paths from one local source; contents are loaded on selection. */
data class KnowledgeDocumentIndex(
    val paths: List<String> = emptyList(),
    val truncated: Boolean = false,
    val error: String? = null,
)
