package app.thdev.moru.feature.terminal

actual fun submitLineToTerminalSession(sessionKey: String, line: String): Boolean =
    JediTermSessionRegistry.find(sessionKey)?.session?.type(line + "\r") ?: false
