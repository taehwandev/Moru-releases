package app.thdev.moru.core.data

import okio.Path

/**
 * Keeps settings reads, writes and updates of one file ordered across every adapter instance in the process, and
 * writes ordered across processes too when [crossProcess] is set (the file is on the real disk).
 */
internal expect class SettingsFileLock(path: Path, crossProcess: Boolean) {
    fun <T> withLock(block: () -> T): T

    fun <T> withWriteLock(block: () -> T): T
}
