package app.thdev.moru.feature.workbench.launch

/**
 * The shell line that starts [runtime] for [prompt], or null when nothing should be typed (a plain terminal).
 * The prompt is wrapped in POSIX single quotes so the shell never expands `$`, backticks or newlines inside it,
 * and an embedded apostrophe is written as `'\''`. A blank prompt, or a runtime that does not take one,
 * starts the runtime's launch command without a prompt. Claude starts conversation [conversationId], so resuming the
 * terminal later reopens exactly this conversation (see [AgentConversation]).
 */
fun agentLaunchCommand(
    runtime: AgentRuntime,
    prompt: String,
    conversationId: String = AgentConversation.newId(),
): String? {
    return app.thdev.moru.core.domain.runtime.AgentLaunchCommands.command(
        app.thdev.moru.core.domain.runtime.PortableAgentRuntime.valueOf(runtime.name), prompt, conversationId,
    )
}
