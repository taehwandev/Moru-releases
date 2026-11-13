package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.feature.workbench.launch.AgentConversation

/** Never borrow another Task's conversation or fall back to an older conversation when the latest is unknown. */
internal fun taskResumeTargets(sessions: List<TerminalSessionSnapshot>, taskKey: String): List<TaskResumeTarget> =
    sessions.asReversed()
        .filter { it.spec.attachedItemId == taskKey && it.spec.kind == TerminalSessionKind.TERMINAL }
        .mapNotNull { session -> launchRuntimeOf(session.spec)?.let { runtime -> runtime to session } }
        .distinctBy { (runtime, _) -> runtime }
        .mapNotNull { (runtime, session) ->
            if (session.status == TerminalSessionStatus.FAILED || session.status == TerminalSessionStatus.STARTING) return@mapNotNull null
            val command = AgentConversation.knownResumeCommandFor(session.spec.startupCommand) ?: return@mapNotNull null
            TaskResumeTarget(runtime, command)
        }
