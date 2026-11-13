package app.thdev.moru.feature.sessions.results

import app.thdev.moru.core.model.session.ConversationTurn

/**
 * The exchanges a result's page shows: the session's [thread], ending with [result] even when the record has not
 * caught up with the screen yet. A thread whose last answer already holds the result ends with it as it is.
 */
internal fun resultTurns(thread: List<ConversationTurn>, result: String?): List<ConversationTurn> {
    if (result.isNullOrBlank()) return thread
    val ending = result.filter(Char::isLetterOrDigit).takeLast(MatchChars)
    val last = thread.lastOrNull()
    if (last != null && ending in last.answer.filter(Char::isLetterOrDigit)) return thread
    return thread + ConversationTurn(null, result)
}

private const val MatchChars = 48
