package app.thdev.moru.core.terminal

/**
 * Finds the address a dev server prints about itself ("Local: http://localhost:5173/") in a stream
 * of terminal output. Only loopback hosts are accepted, so the address can be opened without
 * leaving the machine; an unspecified address (0.0.0.0) is rewritten to localhost. Output arrives in
 * arbitrary chunks, so a short tail is kept to join an address split across two chunks.
 */
class LocalServerUrlDetector {
    private var tail = ""

    /** Feeds one chunk and returns the last loopback address it completed, or null if none. */
    fun accept(chunk: String): String? {
        val text = tail + AnsiSequence.replace(chunk, "")
        var found: String? = null
        for (match in UrlPattern.findAll(text)) {
            // An address at the very end may continue in the next chunk; wait for it.
            if (match.range.last == text.lastIndex && !text.endsWith('\n')) continue
            found = normalize(match.value)
        }
        tail = text.takeLast(MaxTailLength)
        return found
    }

    private fun normalize(raw: String): String {
        val trimmed = raw.trimEnd('.', ',', ';', ')', ']', '\'', '"')
        return trimmed.replace("//0.0.0.0", "//localhost")
    }

    private companion object {
        const val MaxTailLength = 120
        val AnsiSequence = Regex("\u001B\\[[0-9;?]*[ -/]*[@-~]|\u001B\\][^\u0007]*\u0007")
        val UrlPattern = Regex("""https?://(?:localhost|127\.0\.0\.1|0\.0\.0\.0|\[::1])(?::\d{1,5})?(?:/[^\s\u001B]*)?""")
    }
}
