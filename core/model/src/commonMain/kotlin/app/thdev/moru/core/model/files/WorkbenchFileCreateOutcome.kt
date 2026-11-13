package app.thdev.moru.core.model.files

/** Result of creating a new project file from the Workbench. */
sealed interface WorkbenchFileCreateOutcome {
    val path: String

    data class Created(override val path: String) : WorkbenchFileCreateOutcome

    data class Failed(
        override val path: String,
        val message: String,
    ) : WorkbenchFileCreateOutcome
}
