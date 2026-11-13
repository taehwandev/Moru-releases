package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.TaskStatus

/**
 * The state a Task row shows. A pending Approval outranks the raw status, matching
 * [app.thdev.moru.core.model.deriveTaskAttention]: a Task waiting on approval is
 * never shown as quietly working. Every other attention level already equals its status.
 */
fun deriveTaskDisplayState(status: TaskStatus, attention: TaskAttention): TaskDisplayState =
    if (attention == TaskAttention.NeedsApproval) {
        TaskDisplayState.NeedsApproval
    } else {
        status.toDisplayState()
    }
