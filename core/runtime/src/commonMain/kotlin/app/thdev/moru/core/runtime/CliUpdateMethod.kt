package app.thdev.moru.core.runtime

/**
 * How an installed agent CLI is updated, read from where its executable really lives ([cliUpdateMethodOf]). Each
 * install keeps its own way and its own release channel: a Homebrew cask or formula is upgraded by Homebrew (the cask
 * name carries the channel, such as `claude-code@latest`), anything else (Claude's native installer, Codex's and AGY's
 * standalone installs) by the CLI's own `update` command, which follows the channel the user set. An npm global
 * package is left to the user: npm does not record which dist-tag it came from, so Moru cannot keep it on its channel.
 */
sealed interface CliUpdateMethod {
    /** The command words, the first being the program to run (`brew` or the CLI itself); null when Moru cannot update. */
    fun arguments(program: String): List<String>?

    data class Homebrew(val name: String, val cask: Boolean) : CliUpdateMethod {
        override fun arguments(program: String) =
            listOf("brew", "upgrade") + (if (cask) listOf("--cask") else emptyList()) + name
    }

    data class Npm(val packageName: String) : CliUpdateMethod {
        override fun arguments(program: String): List<String>? = null
    }

    data object SelfUpdate : CliUpdateMethod {
        override fun arguments(program: String) = listOf(program, "update")
    }
}
