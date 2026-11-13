package app.thdev.moru.core.runtime

/**
 * Interactive Codex sessions in Moru's PTYs use their own server instead of the shared background server.
 * Codex CLI 0.156+ supports this option. It avoids feature-discovery failures without restarting a daemon
 * that may own other clients' work or changing the user's persistent Codex configuration.
 */
object CodexInteractiveCommands {
    const val start = app.thdev.moru.core.domain.runtime.AgentLaunchCommands.CodexStart
}
