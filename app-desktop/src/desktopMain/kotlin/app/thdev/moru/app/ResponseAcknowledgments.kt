package app.thdev.moru.app

import app.thdev.moru.core.terminal.SessionResponseAttention
import java.io.File

/**
 * The finished responses the user has seen, one session id per line in the app data folder. The terminals outlive
 * Moru in their host, so without this every quiet agent would come back from a restart as a new "Done".
 */
internal class ResponseAcknowledgments(directory: File) {
    private val file = File(directory, "seen-responses.txt")

    fun read(): Set<String> = runCatching { file.readLines() }.getOrDefault(emptyList())
        .map(String::trim).filter(String::isNotEmpty).toSet()

    fun write(sessionIds: Set<String>) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(sessionIds.sorted().joinToString("\n"))
        }
    }

    /** Puts the saved acknowledgments back; call before the sessions are first read. */
    fun restore() = SessionResponseAttention.restoreAcknowledged(read())
}
