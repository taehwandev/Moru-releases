package app.thdev.moru.feature.git.components.branches

/** One command in a ref's right-click menu. [tag] names it for tests; a disabled item stays listed so it can be found. */
internal data class GitRefMenuItem(
    val label: String,
    val tag: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)
