package app.thdev.moru.core.runtime

import java.util.concurrent.ConcurrentHashMap

/**
 * When Moru last updated each agent CLI, by program name (`claude`, `codex`, `agy`), so agent sessions started before
 * then can be restarted on the new version. Kept outside any screen because the update runs in Settings while the
 * sessions live in the workbench.
 */
object CliUpdates {
    private val updatedAt = ConcurrentHashMap<String, Long>()

    fun record(program: String, atMillis: Long) {
        updatedAt[program] = atMillis
    }

    fun updatedAt(program: String): Long? = updatedAt[program]
}
