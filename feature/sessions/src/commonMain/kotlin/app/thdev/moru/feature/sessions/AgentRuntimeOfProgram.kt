package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentRuntime

/** The runtime for a program name: a plain shell when empty, the agents by their display names, else the name as is. */
fun agentRuntimeOfProgram(program: String): AgentRuntime =
    when (program) {
        "" -> AgentRuntime.Terminal
        "claude" -> AgentRuntime.Named("Claude")
        "codex" -> AgentRuntime.Named("Codex")
        "agy", "antigravity" -> AgentRuntime.Named("AGY")
        else -> AgentRuntime.Named(program)
    }
