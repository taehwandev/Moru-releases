package app.thdev.moru.core.data.agent

import app.thdev.moru.core.model.AgentConversationRef
import app.thdev.moru.core.model.session.ConversationTurn
import java.io.File
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The exchanges of a conversation, oldest first, from the transcript its CLI keeps ([agentTranscriptReply] reads only
 * the last answer of the same file): each prompt the user typed and everything the agent wrote back to it. Tool calls,
 * tool output and the CLI's own injected context are left out; a helper's turns inside the user's turn are not the
 * user's. Empty for any other agent or an unreadable transcript. Reads only the end of the file; call it off the UI
 * thread.
 */
fun agentTranscriptThread(conversation: AgentConversationRef, home: File = File(System.getProperty("user.home"))): List<ConversationTurn> {
    val id = conversation.id?.takeIf(String::isNotBlank) ?: return emptyList()
    return when (conversation.program) {
        "claude" -> claudeTranscript(home, id)?.let { claudeThread(tailLines(it)) }
        "codex" -> codexTranscript(home, id)?.let { codexThread(tailLines(it)) }
        else -> null
    }.orEmpty().takeLast(MaxTurns)
}

internal fun claudeThread(lines: List<String>): List<ConversationTurn> {
    val turns = mutableListOf<ConversationTurn>()
    var ask: String? = null
    val answer = mutableListOf<String>()
    fun close() {
        if (answer.isNotEmpty()) turns += ConversationTurn(ask, answer.joinToString("\n\n"))
        answer.clear()
    }
    lines.forEach { line ->
        val entry = parse(line) ?: return@forEach
        if ((entry["isSidechain"] as? JsonPrimitive)?.content == "true" || (entry["isMeta"] as? JsonPrimitive)?.content == "true") return@forEach
        val message = entry["message"] as? JsonObject ?: return@forEach
        when (entry.text("type")) {
            "user" -> claudePrompt(message["content"])?.let { prompt -> close(); ask = prompt }
            "assistant" -> (message["content"] as? JsonArray)?.forEach { block ->
                (block as? JsonObject)?.takeIf { it.text("type") == "text" }?.text("text")?.trim()
                    ?.takeIf(String::isNotEmpty)?.let(answer::add)
            }
        }
    }
    close()
    return turns
}

/** What the user typed, or null for a tool result, a slash command's echo or context the CLI added on its own. */
private fun claudePrompt(content: JsonElement?): String? {
    val texts = when (content) {
        is JsonPrimitive -> listOfNotNull(content.takeIf { it.isString }?.content)
        is JsonArray -> {
            if (content.any { (it as? JsonObject)?.text("type") == "tool_result" }) return null
            content.mapNotNull { block -> (block as? JsonObject)?.takeIf { it.text("type") == "text" }?.text("text") }
        }
        else -> emptyList()
    }
    return texts.map(String::trim).filter { it.isNotEmpty() && !it.isInjected() }.joinToString("\n\n").takeIf(String::isNotEmpty)
}

internal fun codexThread(lines: List<String>): List<ConversationTurn> {
    val asks = linkedMapOf<String, MutableList<String>>()
    val ended = mutableSetOf<String>()
    val turns = mutableListOf<ConversationTurn>()
    lines.forEach { line ->
        val payload = parse(line)?.get("payload") as? JsonObject ?: return@forEach
        when (payload.text("type")) {
            "message" -> {
                if (payload.text("role") != "user") return@forEach
                val turn = (payload["internal_chat_message_metadata_passthrough"] as? JsonObject)?.text("turn_id") ?: return@forEach
                // A compacted history replays earlier prompts under their own turns, which have already ended.
                if (turn in ended) return@forEach
                (payload["content"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.text("text")?.trim() }
                    ?.filter { it.isNotEmpty() && !it.isInjected() }
                    ?.forEach { text -> asks.getOrPut(turn) { mutableListOf() }.let { if (text !in it) it += text } }
            }
            "task_complete" -> {
                val turn = payload.text("turn_id") ?: return@forEach
                val root = payload.text("root_turn_id")
                if (root != null && root != turn) return@forEach
                ended += turn
                val answer = payload.text("last_agent_message")?.trim()?.takeIf(String::isNotEmpty) ?: return@forEach
                turns += ConversationTurn(asks.remove(turn)?.joinToString("\n\n"), answer)
            }
        }
    }
    return turns
}

/** Context a CLI adds to the user's side of the conversation: instructions files, environment, reminders, command echoes. */
private fun String.isInjected(): Boolean =
    startsWith("<") || startsWith("# AGENTS.md") || startsWith("Caveat:") || startsWith("[Request interrupted")

private const val MaxTurns = 40
