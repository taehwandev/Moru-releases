package app.thdev.moru.core.data.agent

/** Reads only native identity operands, stopping before positional prompt text or an unknown option. */
internal fun explicitAgentConversationId(program: String, arguments: List<String>): String? {
    val words = arguments.dropWhile { it.startsWith("--no-warnings") }
        .let { if (it.firstOrNull()?.let { path -> isAgentScript(path, program) } == true) it.drop(1) else it }
    val identityOptions = when (program) {
        "claude" -> setOf("--session-id", "--resume", "-r")
        "agy" -> setOf("--conversation")
        "codex" -> emptySet()
        else -> return null
    }
    val ids = mutableSetOf<String>()
    var resumed = false
    var index = 0
    while (index < words.size) {
        val word = words[index]
        if (word in setOf("--last", "--latest", "--continue", "--fork-session", "--fork") ||
            word == "-c" && program == "claude") return null
        when {
            word in identityOptions || identityOptions.any { word.startsWith("$it=") } -> {
                val id = if ('=' in word) word.substringAfter('=') else words.getOrNull(++index)
                if (id == null || !NativeId.matches(id)) return null
                ids += id
            }
            program == "codex" && word in setOf("exec", "e") && !resumed -> Unit
            program == "codex" && word in setOf("resume", "r") && !resumed -> resumed = true
            word.substringBefore('=') in ValueOptions.getValue(program) -> {
                if ('=' !in word && words.getOrNull(++index) == null) return null
            }
            word in BooleanOptions.getValue(program) -> Unit
            word.startsWith("-") -> return null
            program == "codex" && resumed && ids.isEmpty() -> {
                if (!NativeId.matches(word)) return null
                ids += word
            }
            else -> {
                // Native parsers may still parse flags after a prompt. Do not silently choose one of two IDs.
                if (words.drop(index + 1).any { later ->
                        later in identityOptions || identityOptions.any { later.startsWith("$it=") } ||
                            later in setOf("--last", "--latest", "--continue", "--fork-session", "--fork")
                    }) return null
                break
            }
        }
        index++
    }
    return ids.singleOrNull()
}

private fun isAgentScript(path: String, program: String): Boolean {
    val name = path.substringAfterLast('/')
    return when (program) {
        "codex" -> name == "codex.js" || '/' in path && name == "codex"
        "claude" -> '/' in path && (name == "claude" || "claude-code/" in path && name == "cli.js")
        "agy" -> '/' in path && name in setOf("agy", "agy-real", "antigravity")
        else -> false
    }
}

private val NativeId = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
private val ValueOptions = mapOf(
    "codex" to setOf(
        "--config", "-c", "--model", "-m", "--profile", "-p", "--sandbox", "-s", "--ask-for-approval", "-a",
        "--cd", "-C", "--color", "--output-last-message", "-o", "--output-schema", "--image", "-i", "--add-dir",
    ),
    "claude" to setOf(
        "--permission-mode", "--model", "--output-format", "--input-format", "--max-turns", "--max-budget-usd",
        "--effort", "--setting-sources",
    ),
    "agy" to setOf("--mode", "--model", "--output-format"),
)
private val BooleanOptions = mapOf(
    "codex" to setOf(
        "--no-daemon", "--skip-git-repo-check", "--json", "--full-auto", "--ephemeral", "--no-alt-screen",
        "--dangerously-bypass-approvals-and-sandbox", "--all",
    ),
    "claude" to setOf(
        "--print", "-p", "--verbose", "--dangerously-skip-permissions", "--include-partial-messages",
    ),
    "agy" to setOf("--print", "-p", "--dangerously-skip-permissions", "--verbose"),
)
