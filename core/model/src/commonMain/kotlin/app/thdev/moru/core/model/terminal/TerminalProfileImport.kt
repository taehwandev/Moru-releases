package app.thdev.moru.core.model.terminal

/** What reading another terminal's settings found. */
sealed interface TerminalProfileImport {
    /** The profile to keep, with what it took and what Moru cannot use. */
    data class Imported(
        val profile: TerminalProfile,
        val applied: List<TerminalImportItem>,
        val skipped: List<TerminalImportItem>,
    ) : TerminalProfileImport

    /** The source app's settings are not on this computer. */
    data object NotFound : TerminalProfileImport

    /** The settings were there but could not be read. */
    data class Failed(val reason: String) : TerminalProfileImport
}
