package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity

/**
 * The words a terminal tab or pane header leads with: what the agent says it is working on, without the project
 * name Codex appends after "¦" (the tab already sits in that project), else the line it is printing, else its title.
 */
fun sessionTabHeadline(row: AgentSessionRow, agentName: String): String =
    row.summary?.substringBefore(ProjectSeparator)?.trim()?.takeIf(String::isNotEmpty)
        ?: (if (row.activity == SessionActivity.Working) row.lastLine else null)
        ?: row.title.ifBlank { agentName }

private const val ProjectSeparator = " ¦ "

/** Preserve meaningful internal whitespace in questions, commands, and output. */
internal fun sessionTextMatches(text: String?, headline: String): Boolean =
    !text.isNullOrBlank() && text.trim() == headline.trim()
