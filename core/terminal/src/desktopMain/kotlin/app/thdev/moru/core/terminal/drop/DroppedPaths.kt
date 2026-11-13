package app.thdev.moru.core.terminal.drop

import java.io.File
import java.net.URI

private val shellSpecial = Regex("""[\s\\'"`()\[\]{}<>|&;*?$!#~]""")

/** A path a shell, Claude Code and Codex all read back as that one file. */
internal fun shellPath(file: File): String = file.absolutePath.replace(shellSpecial) { "\\" + it.value }

/** Paths separated and followed by a space, so the next drop or typed word does not join the last path. */
internal fun pastedPaths(files: List<File>): String = files.joinToString(" ", postfix = " ", transform = ::shellPath)

/** The links in dropped text when every non-comment line is one absolute URI, or null when it is plain text. */
internal fun droppedUris(text: String): List<URI>? {
    val lines = text.lineSequence().map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
    if (lines.isEmpty()) return null
    val uris = lines.map { line -> runCatching { URI(line) }.getOrNull() ?: return null }
    return uris.takeIf { list -> list.all { it.scheme in droppableSchemes } }
}

private val droppableSchemes = setOf("file", "http", "https")
