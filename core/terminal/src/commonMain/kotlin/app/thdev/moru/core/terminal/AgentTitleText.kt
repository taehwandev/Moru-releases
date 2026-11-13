package app.thdev.moru.core.terminal

/** [raw] without its leading spinner or status glyphs, or null when it names no work. */
fun agentTitleText(raw: String): String? {
    val clean = raw.filter { char -> !char.isISOControl() }.trim()
    if (clean.startsWith("~") || clean.startsWith("/") || ShellPrompt.matches(clean)) return null
    // Spinners and marks lead the title as their own short words ("✳", "⠸", "π"); the work follows them.
    val words = clean.split(' ').filter(String::isNotEmpty)
    val text = words.dropWhile { word -> word.none(Char::isLetterOrDigit) || (word.length == 1 && word[0].code > 0x7F) }
        .joinToString(" ")
    if (text.isEmpty() || text.lowercase() in GenericTitles) return null
    return if (text.length > TitleLimit) text.take(TitleLimit - 1) + "…" else text
}

private val GenericTitles = setOf("claude", "claude code", "codex", "gemini", "antigravity", "agy", "zsh", "bash", "sh", "fish", "login")
private val ShellPrompt = Regex("""^[\w.-]+@[\w.-]+:.*""")
private const val TitleLimit = 120
