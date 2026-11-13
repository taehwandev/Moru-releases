package app.thdev.moru.feature.terminal

actual fun releaseTerminalSession(sessionKey: String) = JediTermSessionRegistry.release(sessionKey)
