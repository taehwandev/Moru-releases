package app.thdev.moru.app

import app.thdev.moru.core.data.agent.exactRunningAgentConversation
import app.thdev.moru.core.model.AgentConversationRef
import app.thdev.moru.core.terminal.SessionProcessFiles
import app.thdev.moru.core.terminal.SessionRunningAgents

/** Desktop wiring for provider identity inspection. The adapters read files and process metadata without agent input. */
internal fun readDesktopAgentConversation(sessionId: String): AgentConversationRef? {
    val process = SessionRunningAgents.readProcess(sessionId) ?: return null
    val tree = SessionProcessFiles.readProcess(process.pid) ?: return null
    return exactRunningAgentConversation(process.pid, tree.files.openFiles, process.program, process.arguments)
}
