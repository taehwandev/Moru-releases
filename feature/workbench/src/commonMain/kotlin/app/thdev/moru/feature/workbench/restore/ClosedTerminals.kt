package app.thdev.moru.feature.workbench.restore

import app.thdev.moru.core.model.AgentConversationRef
import app.thdev.moru.core.model.SavedTerminal
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.feature.workbench.launch.AgentConversation
import app.thdev.moru.feature.workbench.launch.AgentRuntime

/**
 * The terminals closed with Cmd+W, oldest first, which Cmd+Shift+T opens again newest first. Closing ended the
 * program, so a reopened terminal starts again in the same Task and folder under the same name, and an agent picks
 * its own conversation up again: the one it was holding when it closed ([running]), else the one its startup command
 * names. Without either it comes back as a plain shell. A Lab comes back under its key, so it shows the same
 * conversation and its CLI resumes it. A Preview is never kept.
 */
object ClosedTerminals {
    /** How many closed terminals are kept; the oldest go first. */
    const val Maximum = 20

    /** [closed] with [session] added as the newest, or unchanged when [session] cannot come back. */
    fun add(closed: List<SavedTerminal>, session: TerminalSessionSnapshot, running: AgentConversationRef? = null): List<SavedTerminal> {
        val spec = session.spec
        val saved = when (spec.kind) {
            TerminalSessionKind.LAB -> SavedTerminal(
                title = spec.title,
                shell = spec.shell,
                workingDirectory = spec.workingDirectory,
                taskId = spec.attachedItemId,
                startupCommand = spec.startupCommand,
                hostKey = spec.hostKey,
                lab = true,
            )
            TerminalSessionKind.TERMINAL -> SavedTerminal(
                title = spec.title,
                shell = spec.shell,
                workingDirectory = spec.workingDirectory,
                taskId = spec.attachedItemId,
                startupCommand = running?.let { resumeCommandOf(it, session) } ?: startupResumeCommand(session),
            )
            else -> return closed
        }
        return (closed + saved).takeLast(Maximum)
    }

    /** The index of the newest terminal in [closed] whose Task [canOpen] allows, or null when none can open. */
    fun newestOpenable(closed: List<SavedTerminal>, canOpen: (taskId: String) -> Boolean): Int? =
        closed.indices.lastOrNull { index -> closed[index].taskId?.let(canOpen) == true }

    /** The Lab keys [closed] still holds, whose conversations must be kept until they open again or drop off. */
    fun labKeys(closed: List<SavedTerminal>): Set<String> = closed.filter { it.lab }.mapNotNull { it.hostKey }.toSet()

    private fun resumeCommandOf(running: AgentConversationRef, session: TerminalSessionSnapshot): String? =
        AgentRuntime.entries.firstOrNull { it.program == running.program && it.program.isNotEmpty() }
            ?.let { runtime ->
                // A temporary inspection failure must not discard an identity already checkpointed for this agent.
                val checkpoint = startupResumeCommand(session)?.takeIf { it.startsWith("${runtime.program} ") }
                if (running.id == null && checkpoint != null) checkpoint else AgentConversation.resumeCommand(runtime, running.id)
            }

    // A terminal whose program had already ended comes back as a shell, as it would after a restart.
    private fun startupResumeCommand(session: TerminalSessionSnapshot): String? {
        val ended = session.status == TerminalSessionStatus.EXITED || session.status == TerminalSessionStatus.FAILED
        val mayStillRun = session.status == TerminalSessionStatus.EXITED && session.exitCode == null
        return session.spec.startupCommand
            ?.takeIf { (!ended || mayStillRun) && AgentConversation.idIn(it) != null }
            ?.let(AgentConversation::resumeCommandFor)
    }
}
