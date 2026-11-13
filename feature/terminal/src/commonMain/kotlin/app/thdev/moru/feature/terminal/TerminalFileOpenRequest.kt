package app.thdev.moru.feature.terminal

/** A file path the user clicked in the terminal behind [sessionKey]; [path] is absolute and may end in `:line`. */
class TerminalFileOpenRequest(val sessionKey: String, val path: String)
