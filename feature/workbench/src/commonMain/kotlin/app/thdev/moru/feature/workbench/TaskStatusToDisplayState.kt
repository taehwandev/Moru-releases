package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.TaskStatus

/** Exhaustive over [TaskStatus]: a new status fails compilation until it is mapped here. */
fun TaskStatus.toDisplayState(): TaskDisplayState =
    when (this) {
        TaskStatus.Understanding -> TaskDisplayState.Understanding
        TaskStatus.Preparing -> TaskDisplayState.Preparing
        TaskStatus.Working -> TaskDisplayState.Working
        TaskStatus.Checking -> TaskDisplayState.Checking
        TaskStatus.NeedsInput -> TaskDisplayState.NeedsInput
        TaskStatus.NeedsApproval -> TaskDisplayState.NeedsApproval
        TaskStatus.Done -> TaskDisplayState.Done
        TaskStatus.Stopped -> TaskDisplayState.Stopped
        TaskStatus.Failed -> TaskDisplayState.Failed
    }
