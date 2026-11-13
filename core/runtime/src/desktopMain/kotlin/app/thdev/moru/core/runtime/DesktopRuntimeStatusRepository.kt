package app.thdev.moru.core.runtime

class DesktopRuntimeStatusRepository : RuntimeStatusRepository {
    private val cliUpdater = DesktopCliUpdater()

    override fun installedAgentClis(): List<AgentCli> = AGENT_CLI_PROVIDERS.mapNotNull { provider ->
        resolveDesktopProgram(providerProgram(provider))?.let { executable -> AgentCli(provider, executable.absolutePath) }
    }

    override fun installedVersion(provider: String): String? = cliUpdater.installedVersion(provider)

    override fun updateCommand(provider: String): String? = cliUpdater.updateCommand(provider)

    override fun runUpdate(provider: String): CliUpdateOutcome? = cliUpdater.runUpdate(provider)
}
