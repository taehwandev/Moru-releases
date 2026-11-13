package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.host.TerminalHostLauncher

actual fun terminateAllTerminalSessions() {
    JediTermSessionRegistry.releaseAll(terminate = true)
    TerminalHostLauncher.terminateAll()
}
