package app.thdev.moru.feature.terminal

actual fun releaseAllTerminalSessions() = JediTermSessionRegistry.releaseAll()
