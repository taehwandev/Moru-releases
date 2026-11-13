package app.thdev.moru.core.data.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The folders the last command of a Codex conversation record names, the most recent first: the folders it works in
 * by [commandFolders] and the folder it was told to run in. Codex records its own folder once per turn and runs each
 * command there unless the command says otherwise, so a relative `cd` is read from that folder. A command naming no
 * folder leaves the earlier one, since it ran wherever Codex already was.
 */
fun codexRecordFolders(recordTail: String): List<String> {
    val lines = recordTail.lines()
    val sessionFolder = lines.asReversed().firstNotNullOfOrNull { line ->
        if (TurnContext !in line) return@firstNotNullOfOrNull null
        CwdField.find(line)?.groupValues?.get(1)?.let(::unescapeJson)?.takeIf { folder -> folder.startsWith("/") }
    }
    return lines.asReversed().asSequence()
        .filter { line -> CodexCall.containsMatchIn(line) }
        .mapNotNull(::codexCallText)
        .map { command ->
            val workdir = WorkdirArgument.findAll(command).lastOrNull()?.groupValues?.get(1)
            (commandFolders(command, workdir ?: sessionFolder) + listOfNotNull(workdir)).distinct()
        }
        .firstOrNull { folders -> folders.isNotEmpty() }
        .orEmpty()
}

private fun codexCallText(line: String): String? = runCatching {
    val payload = (Json.parseToJsonElement(line) as? JsonObject)?.get("payload") as? JsonObject
    ((payload?.get("input") ?: payload?.get("arguments")) as? JsonPrimitive)?.content
}.getOrNull()

private const val TurnContext = "\"type\":\"turn_context\""
private val CodexCall = Regex(""""type":"(?:custom_tool_call|function_call)"""")
private val WorkdirArgument = Regex("""workdir"?\s*:\s*\\?["']([^"'\\]+)""")
