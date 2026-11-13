package app.thdev.moru.core.domain.markdown

private val inlineToken = Regex("""(`+)(.+?)\1|\[\[([^\[\]]+)]]|(!?)\[([^\[\]]*)]\((<[^>]+>|(?:\\.|[^\s()\\]|\([^()]*\))+)(?:\s+"[^"]*")?\)|<((?:https?://)[^>]+)>|(\*\*\*|___|\*\*|__|\*|_)(.+?)\8|(https?://[^\s<>`"]+)""", RegexOption.IGNORE_CASE)
private val bareWebLink = Regex("""https?://[^\s<>`"]+""", RegexOption.IGNORE_CASE)

fun markdownRuns(text: String): List<MarkdownRun> = styledRuns(text, 0)

private fun styledRuns(text: String, depth: Int, autoLink: Boolean = true): List<MarkdownRun> {
    if (text.isEmpty()) return emptyList()
    if (depth >= 8) return listOf(MarkdownRun(text))
    val runs = mutableListOf<MarkdownRun>()
    var cursor = 0
    for (match in inlineToken.findAll(text)) {
        val start = match.range.first
        if (start > 0 && text[start - 1] == '\\') continue
        val marker = match.groupValues[8]
        if (marker.startsWith('_') && start > 0 && text[start - 1].isLetterOrDigit()) continue
        if (match.groupValues[10].isNotEmpty() && start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] in "_@\\")) continue
        if (start > cursor) runs += plainRuns(text.substring(cursor, start), autoLink)
        val groups = match.groupValues
        when {
            groups[1].isNotEmpty() -> runs += MarkdownRun(groups[2], code = true)
            groups[3].isNotEmpty() -> {
                val parts = groups[3].split('|', limit = 2)
                val target = parts[0].trim()
                if (target.isEmpty()) runs += MarkdownRun(match.value)
                else runs += styledRuns(parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: target, depth + 1, autoLink = false)
                    .map { it.copy(target = target) }
            }
            groups[4] == "!" -> runs += MarkdownRun(groups[5], imageSource = linkDestination(groups[6]))
            groups[6].isNotEmpty() -> runs += styledRuns(groups[5], depth + 1, autoLink = false).map { it.copy(target = linkDestination(groups[6])) }
            groups[7].isNotEmpty() -> runs += MarkdownRun(groups[7], target = groups[7])
            groups[10].isNotEmpty() -> runs += plainRuns(match.value, autoLink)
            else -> runs += styledRuns(groups[9], depth + 1, autoLink).map {
                it.copy(bold = it.bold || marker.length >= 2, italic = it.italic || marker.length != 2)
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) runs += plainRuns(text.substring(cursor), autoLink)
    return runs
}

private fun linkDestination(value: String): String = value.removeSurrounding("<", ">")
    .replace(Regex("""\\([\\() ])"""), "$1")

/** Only unmatched prose is auto-linked: code and explicit link destinations keep their meaning. */
private fun plainRuns(text: String, autoLink: Boolean): List<MarkdownRun> {
    if (!autoLink) return listOf(MarkdownRun(text))
    val runs = mutableListOf<MarkdownRun>()
    var cursor = 0
    for (match in bareWebLink.findAll(text)) {
        val start = match.range.first
        if (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] in "_@\\")) continue
        val target = trimWebLinkPunctuation(match.value)
        if (target.length <= target.indexOf("://") + 3) continue
        if (start > cursor) runs += MarkdownRun(text.substring(cursor, start))
        runs += MarkdownRun(target, target = target)
        cursor = start + target.length
    }
    if (cursor < text.length) runs += MarkdownRun(text.substring(cursor))
    return runs
}

private fun trimWebLinkPunctuation(value: String): String {
    var target = value.trimEnd('.', ',', ';', ':', '!', '?', '\'', '"')
    while (target.isNotEmpty()) {
        val closing = target.last()
        val opening = when (closing) { ')' -> '('; ']' -> '['; '}' -> '{'; else -> break }
        if (target.count { it == closing } <= target.count { it == opening }) break
        target = target.dropLast(1).trimEnd('.', ',', ';', ':', '!', '?', '\'', '"')
    }
    return target
}
