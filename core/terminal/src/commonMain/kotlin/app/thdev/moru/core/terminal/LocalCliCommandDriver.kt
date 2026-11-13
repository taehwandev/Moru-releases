package app.thdev.moru.core.terminal

import kotlinx.coroutines.delay
import kotlin.time.TimeSource

/** One native slash command, gated by the rendered composer and its completion entry. */
class LocalCliCommandDriver(private val provider: String, private val command: String) {
    enum class Step { Wait, TypeCommand, PressEnter, Complete }
    private var phase = 0
    private var echoedAt: Long? = null

    init {
        require(provider in setOf("codex", "claude", "agy"))
        require(command in setOf("/status", "/usage", "/login", "/logout"))
    }

    /** Bounded to its UI owner's coroutine. No Enter retries or menu selections. */
    suspend fun execute(screen: () -> List<String>?, type: (String) -> Boolean): String? {
        val started = TimeSource.Monotonic.markNow()
        while (started.elapsedNow().inWholeMilliseconds < 30_000) {
            screen()?.let { rows ->
                when (advance(rows, started.elapsedNow().inWholeMilliseconds)) {
                    Step.TypeCommand -> if (!type(command)) return "The CLI command could not be delivered. Close this result and try again."
                    Step.PressEnter -> if (!type("\r")) return "The CLI could not execute this command. Close this result and try again."
                    Step.Complete -> return null
                    Step.Wait -> Unit
                }
            }
            delay(50)
        }
        return "The CLI did not show the command result. Check its startup or login prompt below, then close this result and try again."
    }

    fun advance(rows: List<String>, nowMillis: Long): Step {
        val lines = rows.map(String::trim)
        val text = lines.joinToString("\n")
        return when (phase) {
            0 -> if (ready(lines, text)) { phase = 1; Step.TypeCommand } else Step.Wait
            1 -> {
                // An unknown slash command must never be submitted as a model prompt.
                val exact = lines.any { composer(it) == command }
                val recognized = lines.any { it.contains(command) && it.substringAfter(command).trim().any(Char::isLetter) }
                if (!exact || !recognized) { echoedAt = null; Step.Wait }
                else if (echoedAt == null) { echoedAt = nowMillis; Step.Wait }
                else if (nowMillis - echoedAt!! >= 200) { phase = 2; Step.PressEnter }
                else Step.Wait
            }
            2 -> if (lines.none { composer(it) == command } && result(text)) { phase = 3; Step.Complete } else Step.Wait
            else -> Step.Complete
        }
    }

    private fun ready(lines: List<String>, text: String): Boolean {
        val identity = when (provider) {
            "codex" -> text.contains("OpenAI Codex") && !text.contains("loading", ignoreCase = true)
            "claude" -> text.contains("Claude Code")
            else -> text.contains("Antigravity", ignoreCase = true) || text.contains("AGY", ignoreCase = true)
        }
        return identity && lines.any { line -> composer(line)?.let { input ->
            input.isEmpty() || input.startsWith("Ask Codex") || input.startsWith("Try ") || input.startsWith("Type ")
        } == true }
    }

    private fun composer(line: String): String? =
        line.takeIf { it.firstOrNull() in setOf('›', '❯', '>') }?.drop(1)?.trim()

    private fun result(text: String): Boolean = when {
        provider == "codex" && command == "/usage" ->
            text.contains("Account usage and resets.") && (text.contains("View analytics") || text.contains("Redeem reset"))
        provider == "codex" && command == "/status" -> text.contains("Model:") && (text.contains("Account:") || text.contains("Session:"))
        provider == "claude" && command == "/status" -> text.contains("Version:") && (text.contains("Account:") || text.contains("Login method:"))
        command == "/usage" -> text.contains("usage", ignoreCase = true) &&
            (text.contains("%") || text.contains("reset", ignoreCase = true) || text.contains("quota", ignoreCase = true)) &&
            !text.contains("Loading usage", ignoreCase = true)
        else -> text.contains("login", ignoreCase = true) || text.contains("logged out", ignoreCase = true)
    }
}
