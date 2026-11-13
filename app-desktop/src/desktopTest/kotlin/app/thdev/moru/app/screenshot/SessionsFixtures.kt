package app.thdev.moru.app.screenshot

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.feature.sessions.agentSessionRows

/** Sessions under the Task "t1" of [AttentionFixtures.sidebarState], covering every session state, plus one that belongs to no Task. */
internal object SessionsFixtures {
    const val NOW_MILLIS = 1_760_000_000_000L
    const val TASK = "t1"

    private fun session(
        id: String,
        task: String?,
        command: String?,
        lastOutputAgoMillis: Long?,
        status: TerminalSessionStatus = TerminalSessionStatus.READY,
        exitCode: Int? = null,
    ) = TerminalSessionSnapshot(
        id = id,
        spec = TerminalSessionSpec(title = "세션 $id", attachedItemId = task, startupCommand = command),
        status = status,
        lastOutputAtMillis = lastOutputAgoMillis?.let { ago -> NOW_MILLIS - ago },
        exitCode = exitCode,
    )

    fun rows(): List<AgentSessionRow> =
        agentSessionRows(
            sessions = listOf(
                session("1", TASK, "claude", lastOutputAgoMillis = 2_000),
                session("2", TASK, "codex exec", lastOutputAgoMillis = 95_000),
                session("3", TASK, "agy", lastOutputAgoMillis = null),
                session("4", TASK, "claude", lastOutputAgoMillis = 600_000, status = TerminalSessionStatus.EXITED, exitCode = 0),
                session("5", TASK, "codex", lastOutputAgoMillis = 3_600_000, status = TerminalSessionStatus.EXITED, exitCode = 1),
                session("6", null, null, lastOutputAgoMillis = 4_000),
            ),
            selectedSessionId = "1",
            nowMillis = NOW_MILLIS,
            previewUrlOf = { id -> "http://localhost:5173/".takeIf { id == "2" } },
            lastLineOf = { id ->
                mapOf(
                    "1" to "Editing src/auth/LoginViewModel.kt",
                    "2" to "Proceed with the migration? (y/n)",
                    "5" to "error: 3 tests failed in PaymentTest",
                )[id]
            },
            // Agents that name their work in the terminal title lead their row with it.
            summaryOf = { id -> mapOf("1" to "Fix the login redirect loop", "4" to "Added regression coverage")[id] },
        )
}
