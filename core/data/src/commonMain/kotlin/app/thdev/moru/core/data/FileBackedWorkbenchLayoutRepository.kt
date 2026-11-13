package app.thdev.moru.core.data

import app.thdev.moru.core.domain.WorkbenchLayoutRepository
import app.thdev.moru.core.model.WorkbenchLayout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.buffer
import kotlin.random.Random

/**
 * The [WorkbenchLayout] as one JSON file. A save writes a sibling file and moves it into place, so a crash or a kill
 * in the middle of a save leaves the previous layout rather than a broken one. A file that can no longer be read is
 * moved aside to [unreadableName] before the empty layout replaces it, so a bad save or a newer build's format never
 * silently costs the user every saved terminal and window.
 */
class FileBackedWorkbenchLayoutRepository(
    private val layoutPath: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val recoveryCommands: () -> Map<String, String> = { emptyMap() },
) : WorkbenchLayoutRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
    }

    override fun loadLayout(): WorkbenchLayout {
        val content = runCatching {
            fileSystem.source(layoutPath).buffer().use { source -> source.readUtf8() }
        }.getOrNull() ?: return WorkbenchLayout()
        return try {
            val layout = json.decodeFromString(WorkbenchLayout.serializer(), content)
            val commands = runCatching(recoveryCommands)
                .onFailure { System.err.println("Moru could not read terminal recovery commands: $it") }
                .getOrDefault(emptyMap())
            layout.copy(terminals = layout.terminals.map { saved ->
                if (saved.lab || saved.webPreview) saved
                else commands[saved.hostKey]?.let { saved.copy(startupCommand = it) } ?: saved
            })
        } catch (_: IllegalArgumentException) {
            setAsideUnreadable()
        } catch (_: SerializationException) {
            setAsideUnreadable()
        }
    }

    // Each save writes its own sibling, so two saves that overlap never move a half-written file into place.
    override fun saveLayout(layout: WorkbenchLayout) {
        val parent = layoutPath.parent ?: return
        fileSystem.createDirectories(parent)
        val pending = parent.resolve("${layoutPath.name}.${Random.nextLong().toULong().toString(16)}.saving")
        try {
            fileSystem.sink(pending).buffer().use { sink -> sink.writeUtf8(json.encodeToString(WorkbenchLayout.serializer(), layout)) }
            fileSystem.atomicMove(pending, layoutPath)
        } finally {
            runCatching { fileSystem.delete(pending, mustExist = false) }
        }
    }

    private fun setAsideUnreadable(): WorkbenchLayout {
        layoutPath.parent?.resolve(unreadableName(layoutPath.name))?.let { aside ->
            runCatching { fileSystem.atomicMove(layoutPath, aside) }
        }
        return WorkbenchLayout()
    }

    companion object {
        fun unreadableName(name: String): String = "$name.unreadable"
    }
}
