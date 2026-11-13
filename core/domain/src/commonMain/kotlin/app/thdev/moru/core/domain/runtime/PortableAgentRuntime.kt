package app.thdev.moru.core.domain.runtime

/** Existing terminal choices, independent of UI and process execution. */
enum class PortableAgentRuntime(val id: String, val displayName: String, val program: String,
                                val passesPrompt: Boolean, val canResume: Boolean) {
    Claude("claude", "Claude", "claude", true, true),
    Codex("codex", "Codex", "codex", true, true),
    Agy("agy", "AGY", "agy", true, true),
    Terminal("terminal", "Terminal", "", false, false);

    val launchCommand: String get() = if (this == Codex) AgentLaunchCommands.CodexStart else program
}
