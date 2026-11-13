package app.thdev.moru.core.terminal

import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionBusyScreens
import app.thdev.moru.core.terminal.SessionQuestions
import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionStatus

/** Output within this window counts as the agent working. */
const val SESSION_WORKING_WINDOW_MILLIS: Long = 8_000

/** Legacy duration retained for callers; elapsed time no longer acknowledges an unseen response. */
const val SESSION_DONE_LINGER_MILLIS: Long = 5 * 60_000

/**
 * How long a session that has printed nothing still counts as starting. A terminal kept from an earlier launch, or
 * one in a Project that is not open, prints nothing until it is shown, and calling it "starting" forever read as
 * the app endlessly loading.
 */
const val SESSION_START_PATIENCE_MILLIS: Long = 15_000

/**
 * Judges a session from its process state and last output time. A clean exit is [SessionActivity.Exited],
 * never "done": the terminal cannot know whether the Task's goal was met. Silence only means [SessionActivity.Quiet]
 * while a program runs in the shell; a shell at its prompt ([hasRunningProgram] false) is [SessionActivity.Idle].
 * A program whose screen asks the user a question ([asksUser]) is [SessionActivity.Asking] at once, even while it
 * still redraws: it will not move until the user answers. A program whose screen says it is at work ([showsBusy]: Claude
 * and Codex keep "esc to interrupt" up for a whole turn) is [SessionActivity.Working] even through a silence, such as a
 * long build it started. A quiet response stays available until the user opens it; its acknowledgment is projected
 * by [SessionResponseAttention], independently of this process observation.
 */
fun deriveSessionActivity(
    snapshot: TerminalSessionSnapshot,
    nowMillis: Long,
    workingWindowMillis: Long = SESSION_WORKING_WINDOW_MILLIS,
    hasRunningProgram: (sessionId: String) -> Boolean? = SessionShellProcesses::observedRunningProgram,
    asksUser: (sessionId: String) -> Boolean = { sessionId -> SessionQuestions.questionFor(sessionId) != null },
    showsBusy: (sessionId: String) -> Boolean = SessionBusyScreens::isBusy,
): SessionActivity {
    val exitCode = snapshot.exitCode
    val stillStarting = snapshot.startedAtMillis?.let { nowMillis - it < SESSION_START_PATIENCE_MILLIS } ?: true
    return when {
        snapshot.status == TerminalSessionStatus.FAILED -> SessionActivity.Failed
        snapshot.status == TerminalSessionStatus.EXITED ->
            if (exitCode != null && exitCode != 0) SessionActivity.Failed else SessionActivity.Exited
        snapshot.status == TerminalSessionStatus.STARTING ->
            if (stillStarting) SessionActivity.Starting else SessionActivity.Idle
        snapshot.spec.kind == TerminalSessionKind.LAB -> {
            SessionReportedActivities.activityFor(snapshot.id) ?: SessionActivity.Idle
        }
        else -> {
            val lastOutput = snapshot.lastOutputAtMillis
            when {
                lastOutput == null -> if (stillStarting) SessionActivity.Starting else SessionActivity.Idle
                hasRunningProgram(snapshot.id) == false -> SessionActivity.Idle
                asksUser(snapshot.id) -> SessionActivity.Asking
                showsBusy(snapshot.id) -> SessionActivity.Working
                nowMillis - lastOutput <= workingWindowMillis -> SessionActivity.Working
                else -> SessionActivity.Quiet
            }
        }
    }
}
