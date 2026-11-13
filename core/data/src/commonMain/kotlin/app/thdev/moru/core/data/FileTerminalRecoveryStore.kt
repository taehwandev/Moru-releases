package app.thdev.moru.core.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.buffer
import kotlin.random.Random

/** Exact resume commands by stable host key, independent of layout writes from an older running app. */
class FileTerminalRecoveryStore(private val path: Path, private val fileSystem: FileSystem = FileSystem.SYSTEM) {
    private val lock = SettingsFileLock(path, crossProcess = fileSystem === FileSystem.SYSTEM)

    fun load(): Map<String, String> = lock.withLock {
        if (!fileSystem.exists(path)) emptyMap()
        else fileSystem.source(path).buffer().use { Json.decodeFromString<Map<String, String>>(it.readUtf8()) }
    }

    /** Merges rather than replacing another instance's observations; a malformed original is never overwritten. */
    fun remember(commands: Map<String, String>) {
        if (commands.isEmpty()) return
        lock.withWriteLock {
            val previous = load()
            val merged = previous.toMutableMap()
            commands.forEach { (key, command) ->
                require(key.isNotBlank() && command.isNotBlank())
                merged.remove(key)
                merged[key] = command
            }
            val retained = merged.entries.toList().takeLast(MaximumRecords).associate { it.toPair() }
            if (retained == previous) return@withWriteLock
            val parent = requireNotNull(path.parent)
            fileSystem.createDirectories(parent)
            val pending = parent.resolve("${path.name}.${Random.nextLong().toULong().toString(16)}.saving")
            try {
                fileSystem.sink(pending).buffer().use { it.writeUtf8(Json.encodeToString(retained)) }
                fileSystem.atomicMove(pending, path)
            } finally {
                fileSystem.delete(pending, mustExist = false)
            }
        }
    }

    private companion object { const val MaximumRecords = 512 }
}
