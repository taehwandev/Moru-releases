package app.thdev.moru.core.data

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import okio.Path

internal actual class SettingsFileLock actual constructor(private val path: Path, private val crossProcess: Boolean) {
    private val lock = locks.computeIfAbsent(path.normalized().toString()) { ReentrantLock() }

    actual fun <T> withLock(block: () -> T): T = lock.withLock(block)

    actual fun <T> withWriteLock(block: () -> T): T = lock.withLock {
        // Only the outermost holder takes the OS lock; the JVM refuses a second lock on the same file.
        if (!crossProcess || lock.holdCount > 1) return@withLock block()
        val file = path.toNioPath().toAbsolutePath()
        file.parent?.let(Files::createDirectories)
        val lockFile = file.resolveSibling("${file.fileName}.lock")
        FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.lock().use { block() }
        }
    }

    private companion object {
        val locks = ConcurrentHashMap<String, ReentrantLock>()
    }
}
