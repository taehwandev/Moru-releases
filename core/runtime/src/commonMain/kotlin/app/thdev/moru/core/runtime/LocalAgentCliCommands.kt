package app.thdev.moru.core.runtime

/** Installed CLIs own authentication and account menus; Moru only selects a fixed local entry point. */
object LocalAgentCliCommands {
    /** Recognizes the first word even with extra arguments, so invalid local commands cannot become AI prompts. */
    fun parse(text: String): LocalAgentCliAction? {
        val word = text.trim().takeWhile { !it.isWhitespace() }.lowercase()
        return LocalAgentCliAction.entries.firstOrNull { word == "/${it.commandName}" }
    }

    fun isExact(text: String, action: LocalAgentCliAction): Boolean =
        text.trim().equals("/${action.commandName}", ignoreCase = true)

    fun actions(provider: String): List<LocalAgentCliAction> = when (provider) {
        "claude", "codex" -> LocalAgentCliAction.entries
        "agy", "agy-real", "antigravity" -> LocalAgentCliAction.entries.filter { it != LocalAgentCliAction.Status }
        else -> emptyList()
    }

    fun launch(provider: String, action: LocalAgentCliAction): LocalAgentCliLaunch? {
        if (action !in actions(provider) || action == LocalAgentCliAction.Clear) return null
        return when (provider) {
            "claude" -> when (action) {
                LocalAgentCliAction.Login -> LocalAgentCliLaunch("exec claude auth login")
                LocalAgentCliAction.Logout -> LocalAgentCliLaunch("exec claude auth logout")
                LocalAgentCliAction.Usage -> LocalAgentCliLaunch("exec claude --print /usage")
                else -> LocalAgentCliLaunch("exec claude", "/${action.commandName}")
            }
            "codex" -> when (action) {
                LocalAgentCliAction.Login -> LocalAgentCliLaunch("exec codex login")
                LocalAgentCliAction.Logout -> LocalAgentCliLaunch("exec codex logout")
                else -> LocalAgentCliLaunch("exec ${CodexInteractiveCommands.start}", "/${action.commandName}")
            }
            else -> if (action == LocalAgentCliAction.Usage) LocalAgentCliLaunch("exec agy -p /usage")
                else LocalAgentCliLaunch("exec agy", "/${action.commandName}")
        }
    }
}
