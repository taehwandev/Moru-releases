package app.thdev.moru.feature.workbench.launch

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Which conversation an agent terminal holds, so resuming it reopens that terminal's own conversation. Tasks often
 * share one project folder, and each CLI's "most recent conversation in this folder" (`claude --continue`,
 * `codex resume --last`, `agy --continue`) handed a Task whichever conversation in the folder was touched last,
 * often another Task's. The id travels in the terminal's startup command, which is saved with the terminal, so it
 * survives restarts without any other record.
 */
object AgentConversation {
    private val safeId = Regex("[A-Za-z0-9][A-Za-z0-9._:-]*")

    /** A new conversation id. Claude takes it at launch (`--session-id`); the other CLIs choose their own. */
    @OptIn(ExperimentalUuidApi::class)
    fun newId(): String = Uuid.random().toString()

    /**
     * The conversation id written in [startupCommand] (`claude --session-id|--resume <id>`, `codex resume <id>`,
     * `agy --conversation <id>`), or null when it names no safe opaque id. A quoted prompt is never read as options.
     */
    fun idIn(startupCommand: String?): String? = rawIdIn(startupCommand)?.takeIf(safeId::matches)

    private fun rawIdIn(startupCommand: String?): String? {
        val words = startupCommand?.substringBefore("||")?.trim()?.split(Regex("\\s+"))
            ?.takeIf { it.firstOrNull()?.isNotEmpty() == true } ?: return null
        val option = when (programOf(words.first())) {
            AgentRuntime.Claude.program -> setOf("--session-id", "--resume", "-r")
            AgentRuntime.Codex.program -> setOf("resume")
            AgentRuntime.Agy.program -> setOf("--conversation")
            else -> return null
        }
        // Ignore options inside a quoted prompt, but keep a quoted/invalid id as a candidate so a failed
        // validation cannot turn an explicit resume into an empty conversation.
        val index = words.takeWhile { '\'' !in it && '"' !in it }.indexOfFirst { it in option }
        return if (index >= 0) words.getOrNull(index + 1) else null
    }

    /**
     * The line that reopens conversation [id] of [runtime]. A known id never falls back to a new conversation:
     * a failed resume must remain visible in the terminal. Without an id it starts an empty conversation;
     * restarting an unused terminal must not open a folder-wide conversation picker. Null for no agent or an
     * invalid explicit id, which must never become shell syntax.
     */
    fun resumeCommand(runtime: AgentRuntime, id: String?): String? {
        if (id != null && !safeId.matches(id)) return null
        return when (runtime) {
            AgentRuntime.Claude -> if (id != null) "claude --resume $id" else "claude"
            AgentRuntime.Codex -> {
                val codex = AgentRuntime.Codex.launchCommand
                if (id != null) "$codex resume $id" else codex
            }
            AgentRuntime.Agy -> if (id != null) "agy --conversation $id" else "agy"
            AgentRuntime.Terminal -> null
        }
    }

    /** The line that reopens the conversation the terminal started with [startupCommand], or null for no agent. */
    fun resumeCommandFor(startupCommand: String?): String? {
        val program = startupCommand?.trim()?.substringBefore(' ')?.let(::programOf)?.takeIf { it.isNotEmpty() }
            ?: return null
        val runtime = AgentRuntime.entries.firstOrNull { it.program == program } ?: return null
        return resumeCommand(runtime, rawIdIn(startupCommand))
    }

    /** A Task Resume action must name its conversation and must not turn failure into a picker or fresh start. */
    fun knownResumeCommandFor(startupCommand: String?): String? {
        if (idIn(startupCommand) == null) return null
        return resumeCommandFor(startupCommand)
    }

    private fun programOf(word: String): String = word.substringAfterLast('/').lowercase()
}
