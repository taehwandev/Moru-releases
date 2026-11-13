package app.thdev.moru.feature.terminal

actual fun pasteIntoTerminalSession(sessionKey: String, text: String): Boolean =
    JediTermSessionRegistry.find(sessionKey)?.session?.paste(text) ?: false
