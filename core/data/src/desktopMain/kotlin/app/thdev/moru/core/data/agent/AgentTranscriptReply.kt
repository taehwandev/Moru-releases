package app.thdev.moru.core.data.agent

import app.thdev.moru.core.model.AgentConversationRef
import java.io.File
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * The last message the agent finished a turn with, read from the transcript its CLI keeps, so a result can be shown
 * whole without asking any model:
 *
 * - Claude appends each message to `~/.claude/projects/<folder>/<id>.jsonl`; the newest `assistant` line with text is
 *   its last word.
 * - Codex appends `task_complete` with `last_agent_message` to `~/.codex/sessions/…/rollout-<time>-<id>.jsonl` when a
 *   turn of its own (not a helper's) ends.
 *
 * Null for any other agent, an unknown conversation, or a transcript without such a message. Reads only the end of the
 * file; call it off the UI thread.
 */
fun agentTranscriptReply(conversation: AgentConversationRef, home: File = File(System.getProperty("user.home"))): String? {
    val id = conversation.id?.takeIf(String::isNotBlank) ?: return null
    return when (conversation.program) {
        "claude" -> claudeTranscript(home, id)?.let { claudeLastReply(tailLines(it)) }
        "codex" -> codexTranscript(home, id)?.let { codexLastReply(tailLines(it)) }
        else -> null
    }
}

internal fun claudeLastReply(lines: List<String>): String? = lines.asReversed().firstNotNullOfOrNull { line ->
    if ("\"assistant\"" !in line) return@firstNotNullOfOrNull null
    val entry = parse(line) ?: return@firstNotNullOfOrNull null
    if (entry.text("type") != "assistant") return@firstNotNullOfOrNull null
    val content = (entry["message"] as? JsonObject)?.get("content") as? JsonArray ?: return@firstNotNullOfOrNull null
    content.mapNotNull { block -> (block as? JsonObject)?.takeIf { it.text("type") == "text" }?.text("text") }
        .joinToString("\n\n").trim().takeIf(String::isNotEmpty)
}

internal fun codexLastReply(lines: List<String>): String? = lines.asReversed().firstNotNullOfOrNull { line ->
    if ("task_complete" !in line) return@firstNotNullOfOrNull null
    val payload = parse(line)?.get("payload") as? JsonObject ?: return@firstNotNullOfOrNull null
    if (payload.text("type") != "task_complete") return@firstNotNullOfOrNull null
    // A helper's turn (a reviewer, a sub-agent) ends inside the user's turn and is not its result.
    val root = payload.text("root_turn_id")
    if (root != null && root != payload.text("turn_id")) return@firstNotNullOfOrNull null
    payload.text("last_agent_message")?.trim()?.takeIf(String::isNotEmpty)
}
