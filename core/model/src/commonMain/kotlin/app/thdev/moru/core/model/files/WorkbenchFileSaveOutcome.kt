package app.thdev.moru.core.model.files

/**
 * Result of writing edited content back to a project file. The Workbench only
 * saves files that were opened as full UTF-8 text, so a save is either an exact
 * write or a described failure.
 */
sealed interface WorkbenchFileSaveOutcome {
    val path: String

    data class Saved(
        override val path: String,
        val bytesWritten: Long,
    ) : WorkbenchFileSaveOutcome

    data class Failed(
        override val path: String,
        val message: String,
    ) : WorkbenchFileSaveOutcome
}
