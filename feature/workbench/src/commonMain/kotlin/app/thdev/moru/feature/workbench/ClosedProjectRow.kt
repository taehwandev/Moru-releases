package app.thdev.moru.feature.workbench

/** A recently closed Project, as the sidebar offers it for reopening or deleting. Reopening goes by [rootPath], deleting by [id]. */
data class ClosedProjectRow(
    val id: String,
    val displayName: String,
    val rootPath: String,
)
