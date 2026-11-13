package app.thdev.moru.feature.sessions.results

/** The message without its last line when that line is [question], which the card shows on its own. */
internal fun String.withoutTrailingQuestion(question: String): String {
    val lines = trimEnd().lines()
    val last = lines.indexOfLast(String::isNotBlank)
    if (last < 0 || lines[last].trim().removeSuffix("…") != question.trim().removeSuffix("…") &&
        !question.trim().removeSuffix("…").let { lines[last].trim().startsWith(it) }) return this
    return lines.take(last).joinToString("\n").trimEnd()
}

/** Headings read as bold lines: a report's "## Changes" should not outweigh the card's own title in a narrow sidebar. */
internal fun String.withSmallHeadings(): String = replace(Heading) { match -> "**${match.groupValues[1].trim()}**" }

private val Heading = Regex("""(?m)^#{1,6}\s+(.+)$""")

/** The first line of a report worth reading in a list: its first words without Markdown marks. */
internal fun resultPreview(text: String): String? = text.lineSequence()
    .map { line -> line.trim().replace(PreviewMarks, "").trim() }
    .firstOrNull { it.isNotEmpty() && !it.all { char -> char in "-=*_|`" } }

private val PreviewMarks = Regex("""^(#{1,6}\s+|[-*+]\s+|>\s*|\d+\.\s+)|\*\*|__|`""")
