package app.thdev.moru.feature.settings

import app.thdev.moru.core.runtime.CliUpdateOutcome

/**
 * What Settings says about one agent CLI's version: the installed version once read, the line that updates it (null
 * when Moru cannot, as for an npm install), whether that update is running, and what the last one did.
 */
internal data class CliUpdateView(
    val installed: String? = null,
    val command: String? = null,
    val running: Boolean = false,
    val outcome: CliUpdateOutcome? = null,
)
