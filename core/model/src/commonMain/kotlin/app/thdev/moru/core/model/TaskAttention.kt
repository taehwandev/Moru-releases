package app.thdev.moru.core.model

/**
 * Derived attention level for a Task row. This is not a second authoritative
 * status field; it is computed from [TaskStatus] and pending Approvals so the
 * sidebar can flag Tasks that need the user without color alone.
 */
enum class TaskAttention {
    None,
    NeedsInput,
    NeedsApproval,
    Failed,
}

/**
 * Derive the attention level. A pending Approval always outranks the raw status
 * so a Task waiting on approval is never shown as quietly working.
 */
fun deriveTaskAttention(
    status: TaskStatus,
    pendingApprovalCount: Int,
): TaskAttention =
    when {
        pendingApprovalCount > 0 || status == TaskStatus.NeedsApproval -> TaskAttention.NeedsApproval
        status == TaskStatus.NeedsInput -> TaskAttention.NeedsInput
        status == TaskStatus.Failed -> TaskAttention.Failed
        else -> TaskAttention.None
    }
