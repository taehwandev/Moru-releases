package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.feature.sessions.i18n.SessionsStrings

/** Silence and a clean exit cannot establish that the Task goal was met. */
internal fun sessionRowHint(row: AgentSessionRow, strings: SessionsStrings): String? =
    if (row.finishedTurn || row.activity != SessionActivity.Quiet) strings.activityHint(row.activity) else null
