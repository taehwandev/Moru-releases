package app.thdev.moru.app

import app.thdev.moru.core.terminal.SessionPermissionChoice
import app.thdev.moru.core.terminal.SessionPermissionRequest
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionAttentionTest {
    @Test fun simultaneousRequestsInTheSameTaskRemainIndependentAndAreNotReannounced() {
        val first = TerminalSessionSnapshot("one", TerminalSessionSpec(title = "Codex Lab", kind = TerminalSessionKind.LAB,
            attachedItemId = "task", startupCommand = "codex"), TerminalSessionStatus.READY)
        val second = first.copy(id = "two")
        val request = SessionPermissionRequest("p1", "Allow Bash?", "pwd", listOf(SessionPermissionChoice("deny", "Deny")))
        val candidates = attentionCandidates(mapOf("task" to "Task"), listOf(first, second), 0,
            questionOf = { null }, permissions = mapOf("one" to request, "two" to request.copy(id = "p2")))
        assertEquals(listOf("one", "two"), candidates.map { it.sessionId })
        val policy = AttentionPolicy { 0 }
        assertEquals(candidates, policy.update(candidates, null, false))
        assertEquals(emptyList(), policy.update(candidates, null, false))
        assertEquals(emptyList(), policy.update(candidates.reversed(), null, false))
    }

    @Test fun liveLabRequestsAreAttentionEvenWithoutTerminalOutput() {
        val lab = TerminalSessionSnapshot("lab", TerminalSessionSpec(title = "Codex Lab", kind = TerminalSessionKind.LAB,
            attachedItemId = "task", startupCommand = "codex"), TerminalSessionStatus.READY)
        val request = SessionPermissionRequest("request-1", "Allow Bash?", "pwd", listOf(SessionPermissionChoice("deny", "Deny")))
        val result = attentionCandidates(mapOf("task" to "Task"), listOf(lab), 0,
            questionOf = { null }, permissions = mapOf("lab" to request)).single()
        assertEquals("request-1", result.permissionRequestId)
        assertEquals("lab", result.sessionId)
        assertEquals(AttentionKind.Asking, result.kind)
    }

    @Test fun consecutiveRequestsInTheSameTaskEachProduceOneReceipt() {
        val policy = AttentionPolicy { 0 }
        val first = AttentionCandidate("task", "Task", AttentionKind.Asking, sessionId = "session", permissionRequestId = "one")
        assertEquals(listOf(first), policy.update(listOf(first), null, false))
        assertEquals(emptyList(), policy.update(listOf(first), null, false))
        val next = first.copy(permissionRequestId = "two")
        assertEquals(listOf(next), policy.update(listOf(next), null, false))
        assertEquals(emptyList(), policy.update(listOf(next), null, false))
    }
}
