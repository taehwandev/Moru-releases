package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity

/**
 * The rows for the list of every session, across every Project and Task: what needs a look first,
 * then what is working, then what rests. Rows of the same weight keep the order they were opened in.
 */
fun List<AgentSessionRow>.byUrgency(): List<AgentSessionRow> =
    sortedBy { row ->
        when {
            row.activity.presentation().needsAttention -> 0
            row.activity == SessionActivity.Working || row.activity == SessionActivity.Starting -> 1
            else -> 2
        }
    }
