package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup
import app.thdev.moru.feature.workbench.TaskRow
import app.thdev.moru.feature.workbench.presentation

/**
 * Lays what a session shows over one persisted Task row. A persisted state the user or Moru
 * already decided (done, approval, input, failed) is never replaced by a guess from terminal
 * output; only the "still going" states follow the live session. A turn that just ended reads as done, like its
 * session row, yet still counts as needing the user: there is a result to look at.
 */
fun TaskRow.withSessionActivity(activity: SessionActivity): TaskRow {
    if (displayState.isDecidedByUserOrMoru()) return this
    val overlaid = activity.toTaskDisplayState()
    val presentation = overlaid.presentation()
    return copy(
        displayState = overlaid,
        group = if (activity == SessionActivity.Quiet) TaskGroup.NeedsAttention else presentation.group,
        attention = if (overlaid == TaskDisplayState.Failed) TaskAttention.Failed else attention,
    )
}

private fun TaskDisplayState.isDecidedByUserOrMoru(): Boolean =
    when (this) {
        TaskDisplayState.Done,
        TaskDisplayState.NeedsInput,
        TaskDisplayState.NeedsApproval,
        TaskDisplayState.Failed,
        -> true
        TaskDisplayState.Understanding,
        TaskDisplayState.Preparing,
        TaskDisplayState.Working,
        TaskDisplayState.Checking,
        TaskDisplayState.Waiting,
        TaskDisplayState.Stopped,
        TaskDisplayState.Unknown,
        -> false
    }

/**
 * A quiet agent finished its turn: done for now, not NeedsInput, which only a question on screen can say. An agent or a
 * shell waiting for the user is [TaskDisplayState.Waiting]; only a terminal that ended is [TaskDisplayState.Stopped].
 */
private fun SessionActivity.toTaskDisplayState(): TaskDisplayState =
    when (this) {
        SessionActivity.Starting -> TaskDisplayState.Preparing
        SessionActivity.Working -> TaskDisplayState.Working
        SessionActivity.Asking -> TaskDisplayState.NeedsInput
        SessionActivity.Quiet -> TaskDisplayState.Done
        SessionActivity.Waiting -> TaskDisplayState.Waiting
        SessionActivity.Idle -> TaskDisplayState.Waiting
        SessionActivity.Exited -> TaskDisplayState.Stopped
        SessionActivity.Failed -> TaskDisplayState.Failed
    }
