package app.thdev.moru.feature.settings

import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.terminal.TerminalProfile

/** Changes the terminal profile, starting from Moru's when there is none; a profile left with nothing set is dropped. */
internal fun AppSettings.withTerminalProfile(edit: (TerminalProfile) -> TerminalProfile): AppSettings {
    val next = edit(terminal.profile ?: TerminalProfile())
    val kept = next.takeUnless { it.copy(source = null) == TerminalProfile() }
    return copy(terminal = terminal.copy(profile = kept))
}

/** Drops every terminal look change, imported or made here; the user's own font and scrollback come back. */
internal fun AppSettings.withMoruTerminalLook(): AppSettings = copy(terminal = terminal.copy(profile = null))
