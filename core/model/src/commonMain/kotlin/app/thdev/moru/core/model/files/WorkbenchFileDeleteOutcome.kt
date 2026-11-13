package app.thdev.moru.core.model.files

/** Result of deleting a project file. */
sealed interface WorkbenchFileDeleteOutcome {
    data class Deleted(val path: String) : WorkbenchFileDeleteOutcome

    data class Failed(val path: String, val message: String) : WorkbenchFileDeleteOutcome
}
