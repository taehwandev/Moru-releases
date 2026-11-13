package app.thdev.moru.core.data.agent

import kotlinx.serialization.json.Json

/*
 * Agent records are JSON written by each CLI. Only a field or two is needed from lines that can be very long, so the
 * readers match the field and decode just its string.
 */

/** A JSON string's contents, quotes excluded. */
internal const val JsonString = """((?:[^"\\]|\\.)*)"""

/** A `"cwd"` field, as Claude writes on every line and Codex once per turn. */
internal val CwdField = Regex(""""cwd":"$JsonString"""")

/** [raw] as the string its JSON escapes stand for, or as written when it is not valid JSON. */
internal fun unescapeJson(raw: String): String =
    runCatching { Json.decodeFromString<String>("\"$raw\"") }.getOrDefault(raw)
