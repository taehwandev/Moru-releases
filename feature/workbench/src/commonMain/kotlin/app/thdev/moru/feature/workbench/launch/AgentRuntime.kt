package app.thdev.moru.feature.workbench.launch

import app.thdev.moru.core.domain.runtime.PortableAgentRuntime

/**
 * What a Task's terminal starts with: an agent CLI Moru can launch, or a plain shell. Any agent works through
 * the terminal alone; this only knows how to launch each one. [passesPrompt] is false when the CLI is started
 * without the request text, so the composer can say so instead of dropping it silently. [launchCommand] includes
 * the runtime's terminal startup options while [program] remains its executable name. [canResume] says whether the
 * CLI can reopen a conversation; [AgentConversation] builds the line that reopens a terminal's own one. [Terminal]
 * has no program: it opens the user's shell and starts nothing in it.
 */
enum class AgentRuntime(private val portable: PortableAgentRuntime) {
    Claude(PortableAgentRuntime.Claude),
    Codex(PortableAgentRuntime.Codex),
    Agy(PortableAgentRuntime.Agy),
    Terminal(PortableAgentRuntime.Terminal);

    val displayName: String get() = portable.displayName
    val program: String get() = portable.program
    val passesPrompt: Boolean get() = portable.passesPrompt
    val canResume: Boolean get() = portable.canResume
    val launchCommand: String get() = portable.launchCommand
}
