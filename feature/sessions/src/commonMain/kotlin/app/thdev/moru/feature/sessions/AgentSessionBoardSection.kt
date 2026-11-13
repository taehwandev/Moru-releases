package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity

/** A plain terminal that is quiet still runs its program; an agent that is quiet has finished its turn. */
fun AgentSessionRow.boardSection(hasPermissionRequest: Boolean = false): SessionBoardSection = when {
    hasPermissionRequest -> SessionBoardSection.NeedsAttention
    else -> boardSectionForActivity()
}

private fun AgentSessionRow.boardSectionForActivity(): SessionBoardSection = when (activity) {
    SessionActivity.Starting, SessionActivity.Working -> SessionBoardSection.Running
    SessionActivity.Quiet -> when {
        awaitsReply -> SessionBoardSection.AwaitingReply
        finishedTurn -> SessionBoardSection.Completed
        else -> SessionBoardSection.Running
    }
    SessionActivity.Asking, SessionActivity.Failed -> SessionBoardSection.NeedsAttention
    SessionActivity.Exited, SessionActivity.Waiting, SessionActivity.Idle -> SessionBoardSection.Ready
}

/**
 * Within a section, what most needs the user first: a question, a failure, a finished turn, then sessions
 * at a prompt and last those that ended. Ties keep the order they were opened in.
 */
internal fun List<AgentSessionRow>.byBoardUrgency(): List<AgentSessionRow> = sortedBy { row ->
    when (row.activity) {
        SessionActivity.Asking -> 0
        SessionActivity.Failed -> 1
        SessionActivity.Quiet -> 2
        SessionActivity.Waiting, SessionActivity.Idle -> 3
        else -> 4
    }
}
