package app.thdev.moru.app

import app.thdev.moru.core.model.AgentConversationRef
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.feature.workbench.launch.AgentConversation
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.restore.savedTerminalsOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Captures exact identities without typing into terminals, and writes their current Task bindings before returning. */
internal suspend fun checkpointTerminalConversations(
    sessions: TerminalSessionManager,
    layout: WorkbenchLayoutKeeper,
    readConversation: (String) -> AgentConversationRef?,
    saveCommands: (Map<String, String>) -> Unit = {},
) {
    val candidates = sessions.listSessions().filter { it.spec.kind == TerminalSessionKind.TERMINAL }
    val commands = withContext(Dispatchers.IO) {
        candidates.mapNotNull { session ->
            val key = session.spec.hostKey ?: return@mapNotNull null
            val conversation = runCatching { readConversation(session.id) }.getOrNull() ?: return@mapNotNull null
            val runtime = AgentRuntime.entries.firstOrNull { it.program == conversation.program && it != AgentRuntime.Terminal }
                ?: return@mapNotNull null
            val id = conversation.id?.takeIf { it.isNotBlank() }
            if (id == null) {
                val previous = AgentConversation.knownResumeCommandFor(session.spec.startupCommand)
                // Keep a same-provider checkpoint on a temporary unknown reading, but never reopen a different provider.
                if (previous?.startsWith("${runtime.program} ") == true) return@mapNotNull null
            }
            val command = AgentConversation.resumeCommand(runtime, id) ?: return@mapNotNull null
            Triple(session.id, key, command)
        }
    }
    val currentKeys = sessions.listSessions().associate { it.id to it.spec.hostKey }
    val eligible = commands.filter { (id, key, _) -> currentKeys[id] == key }
    // Publish the sidecar first. Until it commits, neither the live metadata nor the layout saver can publish a
    // newer command that an older sidecar would replace on restart. The previous checkpoint survives write failure.
    val stored = withContext(Dispatchers.IO) {
        runCatching { if (eligible.isNotEmpty()) saveCommands(eligible.associate { (_, key, command) -> key to command }) }
            .onFailure { System.err.println("Moru could not checkpoint terminal conversations: $it") }
            .isSuccess
    }
    var changed = false
    if (stored) eligible.forEach { (id, key, command) ->
        if (sessions.recordResumeCommand(id, key, command)) changed = true
    }
    if (changed) {
        val current = sessions.listSessions()
        val selected = current.indexOfFirst { it.id == sessions.selectedSessionId() }.takeIf { it >= 0 }
        layout.update { it.copy(terminals = savedTerminalsOf(current), selectedTerminal = selected) }
    }
    withContext(Dispatchers.IO) {
        layout.flush()
    }
}
