package app.thdev.moru.core.model.git

data class GitBranchInfo(
    val name: String,
    val isCurrent: Boolean = false,
    val isRemote: Boolean = false,
    val upstream: String? = null,
    val checkedOutPath: String? = null,
    val isMerged: Boolean = false,
)
