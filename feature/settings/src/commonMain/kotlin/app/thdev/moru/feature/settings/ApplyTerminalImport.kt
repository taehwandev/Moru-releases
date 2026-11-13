package app.thdev.moru.feature.settings

import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.terminal.TerminalProfile
import app.thdev.moru.core.model.terminal.TerminalProfileImport

/** Keeps the imported profile on top of the user's own settings, which stay as they were underneath. */
internal fun AppSettings.withTerminalImport(imported: TerminalProfileImport.Imported): AppSettings =
    copy(terminal = terminal.copy(profile = imported.profile))

/** The user picked a terminal font: it becomes their own and the imported one stops standing in for it. */
internal fun AppSettings.withTerminalFont(family: String?, size: Int): AppSettings = copy(
    appearance = appearance.copy(terminalFontFamily = family, terminalFontSize = size),
    terminal = terminal.copy(profile = terminal.profile?.copy(fontFamily = null, fontSize = null)),
)

/** The user set a scrollback length: it becomes their own and the imported one stops standing in for it. */
internal fun AppSettings.withScrollback(lines: Int): AppSettings = copy(
    terminal = terminal.copy(
        scrollbackLines = lines,
        profile = terminal.profile?.copy(scrollbackLines = null),
    ),
)
