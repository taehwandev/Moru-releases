package app.thdev.moru.core.domain

/** Outcome of [ProjectCatalogRepository.relocateProject]. */
enum class ProjectRelocation {
    /** The Project now points at the new folder and keeps its Tasks. */
    Moved,

    /** The folder already belongs to another Project, open or closed. */
    FolderInUse,

    /** The chosen folder does not exist or cannot be read. */
    FolderUnavailable,

    /** No Project has that id. */
    NotFound,
}
