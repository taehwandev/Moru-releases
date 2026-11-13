package app.thdev.moru.core.model

/**
 * The authoritative visible Task state. Precise runtime detail remains available
 * on child descriptors and Runs; this is what the sidebar and conversation show.
 */
enum class TaskStatus {
    Understanding,
    Preparing,
    Working,
    Checking,
    NeedsInput,
    NeedsApproval,
    Done,
    Stopped,
    Failed,
}

val TaskStatus.wireValue: String
    get() = when (this) {
        TaskStatus.Understanding -> "understanding"
        TaskStatus.Preparing -> "preparing"
        TaskStatus.Working -> "working"
        TaskStatus.Checking -> "checking"
        TaskStatus.NeedsInput -> "needs_input"
        TaskStatus.NeedsApproval -> "needs_approval"
        TaskStatus.Done -> "done"
        TaskStatus.Stopped -> "stopped"
        TaskStatus.Failed -> "failed"
    }

fun taskStatusFromWireValue(value: String): TaskStatus =
    when (value) {
        "understanding" -> TaskStatus.Understanding
        "preparing" -> TaskStatus.Preparing
        "working" -> TaskStatus.Working
        "checking" -> TaskStatus.Checking
        "needs_input" -> TaskStatus.NeedsInput
        "needs_approval" -> TaskStatus.NeedsApproval
        "done" -> TaskStatus.Done
        "stopped" -> TaskStatus.Stopped
        "failed" -> TaskStatus.Failed
        else -> TaskStatus.Understanding
    }
