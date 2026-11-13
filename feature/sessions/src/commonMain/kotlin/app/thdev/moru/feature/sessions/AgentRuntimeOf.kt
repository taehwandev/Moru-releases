package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec

/**
 * What a session runs, read from the first word of its startup command so no agent needs to
 * cooperate. A session without a startup command is a plain shell.
 */
fun agentRuntimeOf(spec: TerminalSessionSpec): AgentRuntime {
    if (spec.kind == TerminalSessionKind.WEB_PREVIEW) {
        return AgentRuntime.WebPreview
    }
    val program = spec.startupCommand
        ?.trim()
        ?.substringBefore(' ')
        ?.substringAfterLast('/')
        ?.lowercase()
        .orEmpty()
    return agentRuntimeOfProgram(program)
}
