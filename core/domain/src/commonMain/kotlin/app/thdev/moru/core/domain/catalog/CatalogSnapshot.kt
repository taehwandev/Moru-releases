package app.thdev.moru.core.domain.catalog

/** Versioned, metadata-only source of truth for catalog mutations. */
data class CatalogSnapshot(
    val generation: Long,
    val projects: List<CatalogProject>,
    val tasks: List<CatalogTask>,
    val selectedProjectId: String?,
    val selectedTaskId: String?,
)
