package app.thdev.moru.core.data.agent

import app.thdev.moru.core.model.AgentConversationRef
import java.io.File

/**
 * Exact identity for one freshly selected outermost provider. Explicit native argv can name a conversation even
 * when a print/resume invocation closes its transcript between reads. Provider-owned files remain authoritative;
 * an ambiguous set of records never falls back to startup arguments. Prompts and descendant process argv are ignored.
 */
fun exactRunningAgentConversation(
    pid: Long,
    openFiles: List<String>,
    program: String,
    arguments: List<String>,
    home: File = File(System.getProperty("user.home")),
): AgentConversationRef? {
    val named = explicitAgentConversationId(program, arguments)
    return readRunningAgentConversation(
        listOf(pid), openFiles, program, emptyList(), home,
        codexStartedAt = { named?.let { CodexProcess(startedAtMillis = 0, namedThread = it) } },
        exactOnly = true, namedConversation = named,
    )
}
