package app.thdev.moru.feature.terminal

/**
 * Types [line] into the terminal behind [sessionKey] and presses Enter, as if the user had typed and sent it.
 * Only for a line the app itself wrote (never the user's or an agent's text) at a moment it knows the shell is at
 * its prompt. Returns false if no such live terminal exists.
 */
expect fun submitLineToTerminalSession(sessionKey: String, line: String): Boolean
