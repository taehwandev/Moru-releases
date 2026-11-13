package app.thdev.moru.core.data.agent

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import java.io.File

/**
 * The Codex thread a `codex` started at [sinceMillis] in one of [folders] works on, from Codex's own thread list
 * (`~/.codex/state_<n>.sqlite`). Codex 0.162 and later keep the thread's record open in a shared daemon outside the
 * terminal, so the terminal's processes do not name it. A folder and process start can only identify a thread when
 * exactly one user thread matches; competing matches must not be attributed to whichever terminal is checked.
 * Read only. Null when no thread matches or the match is ambiguous.
 */
fun codexThreadSince(database: File, folders: List<String>, sinceMillis: Long): String? {
    if (!database.isFile || folders.isEmpty()) return null
    return runCatching {
        BundledSQLiteDriver().open(database.path, SQLITE_OPEN_READONLY).use { connection ->
            val marks = folders.joinToString(",") { "?" }
            connection.prepare(
                // Only the user's own threads: a review or other helper Codex starts inside one is not what the terminal shows.
                "SELECT id FROM threads WHERE archived = 0 AND thread_source = 'user' AND cwd IN ($marks) AND updated_at_ms >= ? " +
                    "LIMIT 2",
            ).use { statement ->
                folders.forEachIndexed { index, folder -> statement.bindText(index + 1, folder) }
                statement.bindLong(folders.size + 1, sinceMillis)
                if (statement.step()) {
                    val id = statement.getText(0)
                    id.takeUnless { statement.step() }
                } else {
                    null
                }
            }
        }
    }.getOrNull()
}
