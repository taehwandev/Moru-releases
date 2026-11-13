package app.thdev.moru.core.model.files

/** Result of renaming (moving) a project file. */
sealed interface WorkbenchFileRenameOutcome {
    data class Renamed(val fromPath: String, val toPath: String) : WorkbenchFileRenameOutcome

    data class Failed(val path: String, val message: String) : WorkbenchFileRenameOutcome
}
