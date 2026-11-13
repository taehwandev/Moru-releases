package app.thdev.moru.core.terminal

/** A file path printed in a terminal line: [path] spans [start] until [end], and may end in `:line` or `:line:column`. */
data class TerminalFilePath(val start: Int, val end: Int, val path: String)
