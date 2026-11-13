package app.thdev.moru.feature.terminal

/**
 * Ends the PTY behind [sessionKey]. The terminal view itself never does this when it leaves
 * the screen: only closing the session's tab does, so a running agent survives navigation.
 */
expect fun releaseTerminalSession(sessionKey: String)
