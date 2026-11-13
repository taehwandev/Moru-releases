package app.thdev.moru.core.runtime

private val CLI_VERSION = Regex("""\d+(?:\.\d+){1,3}(?:-[0-9A-Za-z.\-]+)?""")

/** The first version number in a CLI's `--version` output: `2.1.293` from `2.1.293 (Claude Code)`, `0.161.0` from `codex-cli 0.161.0`. */
fun parseCliVersion(output: String): String? = CLI_VERSION.find(output)?.value
