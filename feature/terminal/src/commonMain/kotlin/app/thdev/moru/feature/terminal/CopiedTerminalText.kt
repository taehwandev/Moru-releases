package app.thdev.moru.feature.terminal

/**
 * Tidies text copied from a terminal [width] columns wide. Every line loses the spaces a program padded it with to
 * the right edge, as other terminals copy. Agent CLIs such as Claude Code and Codex also wrap a long command
 * themselves and indent the rest under it, so a two-line command would paste as two lines with a wide gap; a line is
 * joined back to the one above when it starts indented and its first word could not have fit on that line. A break
 * the program made on purpose (the next word would have fit) stays, so copied code keeps its lines and indentation.
 */
fun tidyCopiedTerminalText(text: String, width: Int): String {
    val lines = text.split('\n').map { line -> line.trimEnd() }
    if (width <= 0 || lines.size < 2) return lines.joinToString("\n")
    val joined = mutableListOf(lines.first())
    for (line in lines.drop(1)) {
        val previous = joined.last()
        if (isWrappedContinuation(previous, line, width)) {
            val lastRow = lastRowLength(previous, width)
            // A word cut at the very edge continues straight on; a break between words gets its one space back.
            val gap = if (lastRow >= width - 1) "" else " "
            joined[joined.lastIndex] = previous + gap + line.trimStart()
        } else {
            joined += line
        }
    }
    return joined.joinToString("\n")
}

private fun isWrappedContinuation(previous: String, line: String, width: Int): Boolean {
    if (previous.isBlank() || line.isBlank()) return false
    if (!line.first().isWhitespace()) return false
    val firstWord = line.trimStart().takeWhile { !it.isWhitespace() }
    return lastRowLength(previous, width) + 1 + firstWord.length > width - WrapSlack
}

/** How much of the last screen row a line used; a line the terminal itself wrapped arrives already joined. */
private fun lastRowLength(line: String, width: Int): Int = (line.length - 1) % width + 1

/** Columns a CLI keeps free at the right edge (a scrollbar or box border) when it wraps. */
private const val WrapSlack = 2
