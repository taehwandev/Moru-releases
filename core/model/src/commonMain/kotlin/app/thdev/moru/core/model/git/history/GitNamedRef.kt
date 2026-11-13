package app.thdev.moru.core.model.git.history

/** A literal public ref inventory entry. Merge state stays unknown unless independently verified. */
data class GitNamedRef(
    val fullName: String,
    val name: String,
    val kind: GitRef.Kind,
    val objectID: String,
    val isCurrent: Boolean,
    val upstream: String?,
    val checkedOutPath: String?,
    val isMerged: Boolean? = null,
)
