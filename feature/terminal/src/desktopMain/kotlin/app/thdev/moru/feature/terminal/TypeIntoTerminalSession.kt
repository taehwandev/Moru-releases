package app.thdev.moru.feature.terminal

actual fun typeIntoTerminalSession(sessionKey: String, text: String): Boolean =
    JediTermSessionRegistry.find(sessionKey)?.session?.type(text) ?: false
