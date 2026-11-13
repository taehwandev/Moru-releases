package app.thdev.moru.core.data.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The folders a Claude conversation record names, the most recent first: those its last tool call that names any works
 * in, then [claudeRecordFolder]. Claude often reaches a worktree without moving into it, through `git -C` or by editing
 * a file by its full path, and its current folder alone never shows that.
 */
fun claudeRecordFolders(recordTail: String): List<String> {
    val current = claudeRecordFolder(recordTail)
    val named = recordTail.lines().asReversed().asSequence()
        .filter { line -> ToolUse in line }
        .map { line -> claudeToolFolders(line, CwdField.find(line)?.let { match -> unescapeJson(match.groupValues[1]) } ?: current) }
        .firstOrNull { folders -> folders.isNotEmpty() }
        .orEmpty()
    return (named + listOfNotNull(current)).distinct()
}

/** The folders the tool calls on one record [line] work in, the last first; edits name their file's folder. */
private fun claudeToolFolders(line: String, base: String?): List<String> {
    val message = runCatching { (Json.parseToJsonElement(line) as? JsonObject)?.get("message") as? JsonObject }.getOrNull()
    val calls = (message?.get("content") as? JsonArray).orEmpty()
        .mapNotNull { item -> item as? JsonObject }
        .filter { item -> (item["type"] as? JsonPrimitive)?.content == "tool_use" }
    return calls.asReversed().flatMap { call ->
        val input = call["input"] as? JsonObject ?: return@flatMap emptyList()
        val command = (input["command"] as? JsonPrimitive)?.content
        val file = EditedFileFields.firstNotNullOfOrNull { field -> (input[field] as? JsonPrimitive)?.content }
            ?.takeIf { (call["name"] as? JsonPrimitive)?.content in EditTools && it.startsWith("/") }
        command?.let { commandFolders(it, base) }.orEmpty() + listOfNotNull(file?.substringBeforeLast('/')?.ifEmpty { "/" })
    }.distinct()
}

private const val ToolUse = "\"type\":\"tool_use\""
private val EditTools = setOf("Edit", "Write", "MultiEdit", "NotebookEdit")
private val EditedFileFields = listOf("file_path", "notebook_path")
