package app.thdev.moru.feature.terminal

/**
 * Pastes [text] into the terminal behind [sessionKey] the way a clipboard paste does, so an agent CLI takes several
 * lines as one message and waits for Enter. Returns false if no such live terminal exists.
 */
expect fun pasteIntoTerminalSession(sessionKey: String, text: String): Boolean
