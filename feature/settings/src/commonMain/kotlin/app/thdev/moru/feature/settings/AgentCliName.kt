package app.thdev.moru.feature.settings

/** The name Settings shows for an agent CLI's provider id. */
internal fun agentCliName(provider: String): String = when (provider) {
    "antigravity" -> "Antigravity"
    "claude" -> "Claude"
    "codex" -> "Codex"
    else -> provider
}
