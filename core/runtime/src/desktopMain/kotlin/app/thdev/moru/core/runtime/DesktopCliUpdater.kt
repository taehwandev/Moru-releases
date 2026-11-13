package app.thdev.moru.core.runtime

import java.io.File
import java.util.concurrent.TimeUnit

private const val UPDATE_TIMEOUT_MINUTES = 10L

/**
 * Reads and updates the agent CLIs on this Mac. An update runs the way the CLI was installed ([cliUpdateMethodOf]),
 * never a guessed package manager, so it stays on the release channel the user chose; Moru never looks up a "newest"
 * version of its own. An update counts as done only when `--version` reports a newer version afterwards; then
 * [CliUpdates] records it so sessions on the old version can be restarted.
 */
internal class DesktopCliUpdater(
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun installedVersion(provider: String): String? {
        if (provider !in AGENT_CLI_PROVIDERS) return null
        val executable = resolveDesktopProgram(providerProgram(provider)) ?: return null
        val result = run(listOf(executable.absolutePath, "--version"), executable, timeoutSeconds = 10) ?: return null
        return result.takeIf { it.exitCode == 0 }?.let { parseCliVersion(it.output) }
    }

    fun updateCommand(provider: String): String? = updatePlan(provider)?.joinToString(" ")

    fun runUpdate(provider: String): CliUpdateOutcome? {
        val words = updatePlan(provider) ?: return null
        val display = words.joinToString(" ")
        val cli = resolveDesktopProgram(providerProgram(provider)) ?: return null
        val tool = resolveTool(words.first(), cli) ?: return CliUpdateOutcome.Failed(display, "${words.first()} not found")
        val before = installedVersion(provider)
        val result = run(listOf(tool.absolutePath) + words.drop(1), cli, timeoutSeconds = UPDATE_TIMEOUT_MINUTES * 60)
            ?: return CliUpdateOutcome.Failed(display, null)
        if (result.exitCode != 0) return CliUpdateOutcome.Failed(display, lastLine(result.output))
        val after = installedVersion(provider)
        return if (after != null && (before == null || isNewerCliVersion(after, before))) {
            CliUpdates.record(providerProgram(provider), nowMillis())
            CliUpdateOutcome.Updated(before, after)
        } else {
            CliUpdateOutcome.Unchanged(after ?: before)
        }
    }

    private fun updatePlan(provider: String): List<String>? {
        if (provider !in AGENT_CLI_PROVIDERS) return null
        val program = providerProgram(provider)
        val executable = resolveDesktopProgram(program) ?: return null
        val realPath = runCatching { executable.toPath().toRealPath().toString() }.getOrDefault(executable.absolutePath)
        return cliUpdateMethodOf(realPath).arguments(program)
    }

    /** `npm` and `brew` sit beside the CLI's own link when they installed it (`/opt/homebrew/bin`, an nvm `bin`). */
    private fun resolveTool(word: String, cli: File): File? {
        if (word == cli.name) return cli
        return File(cli.parentFile, word).takeIf { it.isFile && it.canExecute() } ?: resolveDesktopProgram(word)
    }

    /**
     * Runs [command] with the CLI's folder first on PATH, so `npm`'s `node` and Homebrew's tools are found from an app
     * started outside a shell. Input is closed: an update that asks for a password fails instead of waiting forever.
     */
    private fun run(command: List<String>, cli: File, timeoutSeconds: Long): RunResult? = runCatching {
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        val path = listOfNotNull(cli.parent, "/opt/homebrew/bin", "/usr/local/bin", System.getenv("PATH"))
        builder.environment()["PATH"] = path.joinToString(File.pathSeparator)
        val process = builder.start()
        process.outputStream.close()
        val output = StringBuilder()
        val reader = Thread {
            process.inputStream.bufferedReader().useLines { lines -> lines.forEach { line -> output.appendLine(line) } }
        }.apply { isDaemon = true; start() }
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching RunResult(-1, "timed out")
        }
        reader.join(2_000)
        RunResult(process.exitValue(), output.toString())
    }.getOrNull()

    private fun lastLine(output: String): String? =
        output.lineSequence().map { it.trim() }.lastOrNull { it.isNotEmpty() }?.take(200)

    private data class RunResult(val exitCode: Int, val output: String)
}
