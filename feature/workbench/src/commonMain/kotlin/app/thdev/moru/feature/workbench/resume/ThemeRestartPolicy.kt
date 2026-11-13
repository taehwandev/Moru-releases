package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.SessionThemeColors
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.deriveSessionActivity
import app.thdev.moru.core.terminal.SessionShellProcesses

/** The chosen stale Codex belongs to this Task; a selected terminal wins over another stale terminal. */
internal fun themeRestartTarget(
    sessions: List<TerminalSessionSnapshot>,
    taskKey: String,
    selectedSessionId: String?,
    isStale: (String) -> Boolean = SessionThemeColors::hasStaleBackground,
): TerminalSessionSnapshot? {
    val candidates = sessions.filter { session ->
        session.spec.attachedItemId == taskKey && session.spec.kind == TerminalSessionKind.TERMINAL &&
            session.status in setOf(TerminalSessionStatus.READY, TerminalSessionStatus.STARTING) &&
            launchRuntimeOf(session.spec) == AgentRuntime.Codex && isStale(session.id)
    }
    return candidates.firstOrNull { it.id == selectedSessionId } ?: candidates.firstOrNull()
}

internal fun themeRestartAllowed(
    session: TerminalSessionSnapshot,
    activity: SessionActivity,
    requestInProgress: Boolean = false,
): Boolean = !requestInProgress && session.status == TerminalSessionStatus.READY &&
    launchRuntimeOf(session.spec) == AgentRuntime.Codex &&
    activity in setOf(SessionActivity.Quiet, SessionActivity.Waiting, SessionActivity.Idle)

/** Re-read the exact terminal before stopping it and before submitting its resume line. */
internal fun themeRestartGuard(
    session: TerminalSessionSnapshot,
    taskKey: String,
    sessionId: String,
    nowMillis: Long,
    activityOf: (TerminalSessionSnapshot, Long) -> SessionActivity = { snapshot, now ->
        deriveSessionActivity(snapshot, now, hasRunningProgram = SessionShellProcesses::hasRunningProgram)
    },
    isStale: (String) -> Boolean = SessionThemeColors::hasStaleBackground,
): Boolean = session.id == sessionId && session.spec.attachedItemId == taskKey &&
    session.spec.kind == TerminalSessionKind.TERMINAL && isStale(session.id) &&
    themeRestartAllowed(session, activityOf(session, nowMillis))
