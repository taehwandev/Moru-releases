package app.thdev.moru.core.runtime

/** An agent CLI found on this machine: Moru's provider id (`claude`, `codex`, `antigravity`) and where it was found. */
data class AgentCli(
    val provider: String,
    val path: String,
)

interface RuntimeStatusRepository {
    /** The agent CLIs installed on this machine, in [AGENT_CLI_PROVIDERS] order. May block. */
    fun installedAgentClis(): List<AgentCli>

    /** The installed version of [provider]'s CLI from its `--version`, or null when unknown. May block. */
    fun installedVersion(provider: String): String? = null

    /** The line that updates [provider]'s CLI the way it was installed, or null when Moru cannot update it. */
    fun updateCommand(provider: String): String? = null

    /** Runs [updateCommand] and reports what changed; null when Moru cannot update it. Blocks until it finishes. */
    fun runUpdate(provider: String): CliUpdateOutcome? = null
}

/** A repository that finds no CLI, for tests and previews that do not probe the machine. */
object StaticRuntimeStatusRepository : RuntimeStatusRepository {
    override fun installedAgentClis(): List<AgentCli> = emptyList()
}

/** The agent CLIs Moru launches in Task terminals, by provider id. */
val AGENT_CLI_PROVIDERS = listOf("claude", "codex", "antigravity")

internal fun providerProgram(provider: String): String {
    return when (provider) {
        "antigravity" -> "agy"
        else -> provider
    }
}
