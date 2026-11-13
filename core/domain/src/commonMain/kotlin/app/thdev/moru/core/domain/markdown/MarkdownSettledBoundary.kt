package app.thdev.moru.core.domain.markdown

/** The start of the line after the last blank line from [from] on that is outside a fence, or [from] when none is. */
fun settledBoundary(text: String, from: Int): Int {
    var boundary = from
    var marker: String? = null
    var start = from
    while (true) {
        val end = text.indexOf('\n', start)
        if (end < 0) return boundary
        val line = text.substring(start, end).removeSuffix("\r")
        val open = marker
        when {
            open != null -> if (isClosingFence(line, open)) marker = null
            line.isBlank() -> boundary = end + 1
            else -> fence.matchEntire(line)?.let { marker = it.groupValues[1] }
        }
        start = end + 1
    }
}
