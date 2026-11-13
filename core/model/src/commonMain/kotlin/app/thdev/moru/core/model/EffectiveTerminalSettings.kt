package app.thdev.moru.core.model

/** The terminal font family in use: the imported profile's while one is kept, else the user's own. */
val AppSettings.effectiveTerminalFontFamily: String?
    get() = terminal.profile?.fontFamily ?: appearance.terminalFontFamily

/** The terminal font size in use: the imported profile's while one is kept, else the user's own. */
val AppSettings.effectiveTerminalFontSize: Int
    get() = terminal.profile?.fontSize ?: appearance.terminalFontSize

/** The scrollback length in use: the imported profile's while one is kept, else the user's own. */
val AppSettings.effectiveScrollbackLines: Int
    get() = terminal.profile?.scrollbackLines ?: terminal.scrollbackLines
