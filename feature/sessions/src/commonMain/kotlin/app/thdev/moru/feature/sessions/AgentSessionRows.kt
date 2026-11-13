package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.terminal.TerminalSessionSnapshot

import app.thdev.moru.core.terminal.deriveSessionActivity
import app.thdev.moru.core.terminal.SessionReplyQuestions
/**
 * Every session as a sidebar row, in the order the terminals were opened. [previewUrlOf], [lastLineOf] and [summaryOf]
 * are asked per session so the caller decides where an address or a printed line comes from. [runningAgentOf] names the
 * agent running in the terminal now, which outranks the one it started with.
 */
fun agentSessionRows(
    sessions: List<TerminalSessionSnapshot>,
    selectedSessionId: String?,
    nowMillis: Long,
    previewUrlOf: (sessionId: String) -> String? = { null },
    lastLineOf: (sessionId: String) -> String? = { null },
    summaryOf: (sessionId: String) -> String? = { null },
    questionOf: (sessionId: String) -> String? = { null },
    replyQuestionOf: (sessionId: String) -> String? = SessionReplyQuestions::questionFor,
    runningAgentOf: (sessionId: String) -> String? = { null },
): List<AgentSessionRow> =
    sessions.map { session ->
        val agent = runningAgentOf(session.id)?.let(::agentRuntimeOfProgram) ?: agentRuntimeOf(session.spec)
        val observedActivity = deriveSessionActivity(session, nowMillis)
        AgentSessionRow(
            sessionId = session.id,
            title = session.title,
            agent = agent,
            activity = if (agent is AgentRuntime.Named) SessionResponseAttention.project(session.id, observedActivity) else observedActivity,
            lastOutputAtMillis = session.lastOutputAtMillis,
            nowMillis = nowMillis,
            exitCode = session.exitCode,
            previewUrl = previewUrlOf(session.id),
            selected = session.id == selectedSessionId,
            lastLine = lastLineOf(session.id),
            summary = summaryOf(session.id),
            taskKey = session.spec.attachedItemId,
            question = questionOf(session.id),
            replyQuestion = replyQuestionOf(session.id),
        )
    }
