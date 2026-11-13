package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/** How many terminals of one Task are drawn side by side. The rest stay alive and are one tab away. */
const val MaxVisibleTerminals = 8

/**
 * The terminals to draw: the first [max] in their own order, so they keep their places when another is selected. A
 * selected terminal that would fall outside them takes the last place, so what the user picked is always on screen.
 */
internal fun visibleTerminalSessions(
    sessions: List<TerminalSessionSnapshot>,
    selectedSessionId: String?,
    max: Int = MaxVisibleTerminals,
): List<TerminalSessionSnapshot> {
    val first = sessions.take(max)
    val selected = sessions.firstOrNull { session -> session.id == selectedSessionId }
    return if (selected == null || selected in first) first else first.dropLast(1) + selected
}
