package app.thdev.moru.core.model.files

data class WorkbenchProjectTreeEntry(
    val name: String,
    val path: String,
    val depth: Int,
    val isFile: Boolean,
    /** Git ignores it (or, outside a repository, it is a usual build or dependency folder). */
    val isIgnored: Boolean = false,
    /** A folder whose children were left out of the scan; they are read when the folder is opened. */
    val isDeferred: Boolean = false,
)
