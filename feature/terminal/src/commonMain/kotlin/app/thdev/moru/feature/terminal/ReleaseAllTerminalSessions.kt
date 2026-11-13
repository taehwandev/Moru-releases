package app.thdev.moru.feature.terminal

/** Ends every PTY this process started. Call once when the app exits so no agent is orphaned. */
expect fun releaseAllTerminalSessions()
