package app.thdev.moru.core.data.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/* One transcript line read as JSON, and its string fields; a line that is not a JSON object reads as null. */

internal fun parse(line: String): JsonObject? = runCatching { Json.parseToJsonElement(line) as? JsonObject }.getOrNull()

internal fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
