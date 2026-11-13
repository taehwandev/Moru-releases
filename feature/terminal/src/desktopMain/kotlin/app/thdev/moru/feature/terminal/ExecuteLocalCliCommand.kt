package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.LocalCliCommandDriver
import app.thdev.moru.core.terminal.terminalScreenRows

/** Waits only during this action; closing its owner cancels it. Never retries an Enter. */
suspend fun executeLocalCliCommand(sessionKey: String, provider: String, command: String): String? {
    return LocalCliCommandDriver(provider, command).execute(
        screen = { JediTermSessionRegistry.find(sessionKey)?.session?.let(::nativeCliRows) },
        type = { text -> JediTermSessionRegistry.find(sessionKey)?.session?.type(text) == true },
    )
}

private fun nativeCliRows(session: JediTermSession): List<String>? {
    val buffer = session.textBuffer
    if (!buffer.tryLock()) return null
    return try { terminalScreenRows(buffer) } finally { buffer.unlock() }
}
