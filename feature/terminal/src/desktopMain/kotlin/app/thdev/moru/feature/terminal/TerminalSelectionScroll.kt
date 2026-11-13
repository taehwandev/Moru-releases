package app.thdev.moru.feature.terminal

import com.jediterm.core.compatibility.Point
import com.jediterm.terminal.model.TerminalSelection

/**
 * Where a selection lands after the screen rows `top..top + size - 1` (1-based) scroll by [dy], so it stays on the
 * text it covered, as Terminal.app keeps it. Agent CLIs scroll while they redraw, and JediTerm drops the selection on
 * every scroll. Null when the selected rows were scrolled away; [intoHistory] when rows scrolled up off the top row
 * go to history, as they do outside the alternate screen.
 */
internal fun TerminalSelection.afterScroll(top: Int, size: Int, dy: Int, intoHistory: Boolean): TerminalSelection? {
    val first = top - 1
    val end = first + size
    val low = minOf(start.y, this.end.y)
    val high = maxOf(start.y, this.end.y)
    val history = intoHistory && dy < 0 && top == 1
    if (low >= end || (high < first && !history)) return this
    val floor = if (history) Int.MIN_VALUE else first
    if (high >= end || low + dy < floor || high + dy >= end) return null
    return TerminalSelection(Point(start.x, start.y + dy), Point(this.end.x, this.end.y + dy))
}
