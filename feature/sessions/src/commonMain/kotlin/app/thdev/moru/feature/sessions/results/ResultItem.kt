package app.thdev.moru.feature.sessions.results

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef

/** What a session needs from the user, or that it finished; it decides the card the Results view draws. */
internal enum class ResultKind { Permission, Question, Failed, Done }

/**
 * One session in the Results view: its row, the Task it belongs to, and [text], the last message its agent finished
 * with, or null when none could be read. [seenKey] names this result, not the session, so the next result of the same
 * session is new again.
 */
internal data class ResultItem(
    val row: AgentSessionRow,
    val task: SessionTaskRef?,
    val text: String?,
    val kind: ResultKind,
) {
    val seenKey: Int
        get() = text?.hashCode() ?: "${row.sessionId}@${row.lastOutputAtMillis}".hashCode()
}
