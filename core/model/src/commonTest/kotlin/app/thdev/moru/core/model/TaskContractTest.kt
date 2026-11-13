package app.thdev.moru.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TaskContractTest {
    @Test
    fun pendingApprovalOutranksWorkingStatusForAttention() {
        assertEquals(
            TaskAttention.NeedsApproval,
            deriveTaskAttention(status = TaskStatus.Working, pendingApprovalCount = 1),
        )
    }

    @Test
    fun needsApprovalStatusDrivesAttentionWithoutPendingRecord() {
        assertEquals(
            TaskAttention.NeedsApproval,
            deriveTaskAttention(status = TaskStatus.NeedsApproval, pendingApprovalCount = 0),
        )
    }

    @Test
    fun needsInputAndFailedMapToTheirAttention() {
        assertEquals(
            TaskAttention.NeedsInput,
            deriveTaskAttention(status = TaskStatus.NeedsInput, pendingApprovalCount = 0),
        )
        assertEquals(
            TaskAttention.Failed,
            deriveTaskAttention(status = TaskStatus.Failed, pendingApprovalCount = 0),
        )
    }

    @Test
    fun quietWorkingStatusHasNoAttention() {
        assertEquals(
            TaskAttention.None,
            deriveTaskAttention(status = TaskStatus.Working, pendingApprovalCount = 0),
        )
    }

    @Test
    fun taskStatusRoundTripsThroughWireValue() {
        TaskStatus.entries.forEach { status ->
            assertEquals(status, taskStatusFromWireValue(status.wireValue))
        }
    }
}
