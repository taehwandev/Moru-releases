package app.thdev.moru.core.data.agent

import app.thdev.moru.core.model.AgentConversationRef
import java.io.File

/**
 * Reads the running agent's conversation from its process session file, named process, or uniquely held record.
 * Intended for automatic checkpoints: a Codex thread is never inferred from a folder or the thread database, since
 * even a unique match could belong to a different terminal. A known [program] without an exact identity keeps a null ID.
 * Competing distinct identities in Claude process files or AGY logs are rejected; repeated evidence for one ID is valid.
 * Nothing here changes provider files. Call off the UI thread.
 */
fun exactRunningAgentConversation(
    pids: List<Long>,
    openFiles: List<String>,
    program: String? = null,
    home: File = File(System.getProperty("user.home")),
    codexStartedAt: (pid: Long) -> CodexProcess? = ::codexProcess,
): AgentConversationRef? = readRunningAgentConversation(
    pids, openFiles, program, emptyList(), home, codexStartedAt, exactOnly = true,
)
