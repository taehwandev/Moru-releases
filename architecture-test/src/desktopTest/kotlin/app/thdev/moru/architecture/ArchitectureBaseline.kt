package app.thdev.moru.architecture

/**
 * Violations that existed when the rules were introduced. Entries are only
 * ever removed: the tests fail when a listed violation is fixed but its entry
 * stays, and when a new violation appears that is not listed.
 */
object ArchitectureBaseline {
    val forbiddenEdges: Set<String> = setOf(
        // Markdown editing helpers in core:ui delegate to the pure editing rules in core:domain.
        ":core:ui -> :core:domain",
    )

    val unusedEdges: Set<String> = emptySet()

    val featureRuntimeAccess: Set<String> = emptySet()

    val appProcessExecution: Set<String> = emptySet()
}
