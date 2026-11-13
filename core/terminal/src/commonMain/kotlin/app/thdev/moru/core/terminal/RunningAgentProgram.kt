package app.thdev.moru.core.terminal

/**
 * Which agent CLI a process is, as `claude`, `codex` or `agy`, read from its executable and, when that is an
 * interpreter, the script it runs (a script named `codex`, npm's `codex.js`, or a file inside `claude-code`). Null for
 * any other program.
 */
fun runningAgentProgram(command: String, arguments: List<String>): String? {
    agentOfName(baseName(command))?.let { return it }
    if (baseName(command) !in Interpreters) return null
    val script = arguments.firstOrNull { !it.startsWith("-") } ?: return null
    if (ClaudePackage in script) return "claude"
    return agentOfName(baseName(script).substringBefore('.'))
}

private fun agentOfName(name: String): String? = when (name.lowercase()) {
    "claude" -> "claude"
    "codex" -> "codex"
    "agy", "agy-real", "antigravity" -> "agy"
    else -> null
}

private fun baseName(path: String): String = path.substringAfterLast('/').substringAfterLast('\\').removeSuffix(".exe")

private const val ClaudePackage = "claude-code"

private val Interpreters = setOf("node", "bun", "deno", "python", "python3", "sh", "bash", "zsh")
