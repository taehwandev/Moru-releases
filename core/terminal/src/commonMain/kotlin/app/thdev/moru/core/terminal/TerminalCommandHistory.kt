package app.thdev.moru.core.terminal

data class TerminalCommandHistoryEntry(
    val id: String,
    val sessionId: String,
    val command: String,
    val workingDirectory: String,
)

internal class TerminalCommandInputParser {
    private val currentLine = StringBuilder()
    private var skippingEscapeSequence = false
    private var skippingControlSequence = false
    private val escapeSequence = StringBuilder()
    private var bracketedPaste = false

    fun accept(input: String, onSubmit: () -> Unit = {}): List<String> {
        val commands = mutableListOf<String>()
        input.forEach { char ->
            when {
                skippingEscapeSequence -> {
                    escapeSequence.append(char)
                    when {
                        char == '[' -> skippingControlSequence = true
                        !skippingControlSequence || char in '@'..'~' -> {
                            when (escapeSequence.toString()) {
                                "[200~" -> bracketedPaste = true
                                "[201~" -> bracketedPaste = false
                            }
                            skippingControlSequence = false
                            skippingEscapeSequence = false
                        }
                    }
                }
                char == '\u001B' -> {
                    escapeSequence.clear()
                    skippingEscapeSequence = true
                    skippingControlSequence = false
                }
                char == '\r' || char == '\n' -> {
                    if (!bracketedPaste) onSubmit()
                    flushCommand()?.let(commands::add)
                }
                char == '\b' || char == '\u007F' -> {
                    if (currentLine.isNotEmpty()) {
                        currentLine.setLength(currentLine.length - 1)
                    }
                }
                char == '\u0003' -> currentLine.clear()
                !char.isISOControl() -> currentLine.append(char)
            }
        }

        return commands
    }

    private fun flushCommand(): String? {
        val command = currentLine.toString().trim()
        currentLine.clear()
        return command.takeIf { value -> value.isNotEmpty() }
    }
}

internal fun TerminalSessionSnapshot.withRecordedCommand(command: String): TerminalSessionSnapshot {
    val normalizedCommand = command.trim()
    if (normalizedCommand.isEmpty()) {
        return this
    }

    val entry = TerminalCommandHistoryEntry(
        id = "cmd-${(commandHistory.size + 1).toString().padStart(3, '0')}",
        sessionId = id,
        command = normalizedCommand,
        workingDirectory = spec.workingDirectory,
    )

    return copy(commandHistory = (commandHistory + entry).takeLast(MaxCommandHistoryEntries))
}

private const val MaxCommandHistoryEntries = 100
