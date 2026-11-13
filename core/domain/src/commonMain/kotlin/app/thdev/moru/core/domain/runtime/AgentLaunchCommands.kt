package app.thdev.moru.core.domain.runtime

/** Copy-first owner of terminal startup rules; callers supply conversation identity. */
object AgentLaunchCommands {
    const val CodexStart = "codex --no-daemon"

    fun command(runtime: PortableAgentRuntime, prompt: String, conversationId: String): String? {
        if (runtime == PortableAgentRuntime.Terminal) return null
        val launch = if (runtime == PortableAgentRuntime.Claude) {
            require(conversationId.isNotEmpty() && conversationId.all {
                it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' || it == '_'
            }) { "Invalid conversation identity" }
            "${runtime.launchCommand} --session-id $conversationId"
        } else runtime.launchCommand
        val text = prompt.trim()
        if (text.isEmpty() || !runtime.passesPrompt) return launch
        val option = if (runtime == PortableAgentRuntime.Agy) " -i" else ""
        return "$launch$option '${text.replace("'", "'\\''")}'"
    }
}
