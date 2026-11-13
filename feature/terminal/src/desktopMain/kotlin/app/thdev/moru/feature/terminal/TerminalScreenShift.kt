package app.thdev.moru.feature.terminal

/**
 * How many rows the screen's text moved between [before] and [after]. Claude Code and Codex scroll their transcript on
 * the alternate screen by redrawing it in place rather than scrolling, so the shift is the offset at which the most
 * non-blank rows line up again. Fixed rows such as an input box line up at 0; 0 also when nothing lines up better.
 */
internal fun screenShift(before: List<String>, after: List<String>): Int {
    val height = minOf(before.size, after.size)
    fun matches(dy: Int) = (0 until height).count { row ->
        row + dy in 0 until height && before[row].isNotBlank() && before[row] == after[row + dy]
    }
    var best = 0
    var bestMatches = matches(0)
    for (distance in 1 until height) for (dy in intArrayOf(distance, -distance)) {
        val count = matches(dy)
        if (count > bestMatches) {
            best = dy
            bestMatches = count
        }
    }
    return best
}
