package app.thdev.moru.core.model

/**
 * A user-registered local Project in the global catalog.
 *
 * A Project id is an opaque stable id and is never derived from a folder path so
 * that a moved or missing root keeps its identity and Task metadata. Project owns
 * registration, display name, root references, and the last selected Task; it does
 * not own file tree contents, watchers, or live runtime children.
 */
data class Project(
    val id: String,
    val displayName: String,
    val roots: List<ProjectRoot>,
    val lastSelectedTaskId: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    val primaryRoot: ProjectRoot?
        get() = roots.firstOrNull { root -> root.role == ProjectRootRole.Primary }
            ?: roots.firstOrNull()

    /** A Project is usable only when its primary root can currently be reached. */
    val availability: ProjectAvailability
        get() = primaryRoot?.availability ?: ProjectAvailability.Missing
}
