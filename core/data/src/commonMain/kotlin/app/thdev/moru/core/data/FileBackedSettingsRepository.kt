package app.thdev.moru.core.data

import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.random.Random

class FileBackedSettingsRepository(
    private val settingsPath: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : SettingsRepository {
    private val lock = SettingsFileLock(settingsPath, crossProcess = fileSystem === FileSystem.SYSTEM)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override fun loadSettings(): AppSettings = lock.withLock { readSettings() }

    private fun readSettings(): AppSettings {
        val content = readUtf8(settingsPath) ?: return AppSettings()
        return decodeOrDefault(content)
    }

    override fun saveSettings(settings: AppSettings) = lock.withWriteLock { writeSettings(settings) }

    override fun updateSettings(transform: (AppSettings) -> AppSettings): AppSettings = lock.withWriteLock {
        // An unreadable or undecodable existing file must fail, rather than merging an edit into defaults.
        val content = if (fileSystem.exists(settingsPath)) {
            fileSystem.source(settingsPath).buffer().use { it.readUtf8() }
        } else null
        val latest = content?.let(::decodeStrict) ?: AppSettings()
        val updated = transform(latest)
        if (updated != latest) writeSettings(updated)
        updated
    }

    private fun writeSettings(settings: AppSettings) {
        settingsPath.parent?.let { parent ->
            fileSystem.createDirectories(parent)
        }

        // A unique name keeps a concurrent writer from replacing or deleting this write's pending file.
        val pendingName = "${settingsPath.name}.${Random.nextLong().toULong().toString(16)}.saving"
        val pending = settingsPath.parent?.resolve(pendingName) ?: pendingName.toPath()
        try {
            fileSystem.sink(pending).buffer().use { sink ->
                sink.writeUtf8(json.encodeToString(settings))
            }
            fileSystem.atomicMove(pending, settingsPath)
        } catch (error: Exception) {
            runCatching { fileSystem.delete(pending, mustExist = false) }
            throw error
        }
    }

    private fun readUtf8(path: Path): String? {
        return runCatching {
            fileSystem.source(path).buffer().use { source ->
                source.readUtf8()
            }
        }.getOrNull()
    }

    private fun decodeStrict(content: String): AppSettings = try {
        json.decodeFromJsonElement(AppSettings.serializer(), json.parseToJsonElement(content))
    } catch (error: IllegalArgumentException) {
        throw IllegalStateException("Settings file $settingsPath is not valid; fix or remove it before changing settings.", error)
    }

    private fun decodeOrDefault(content: String): AppSettings {
        return try {
            json.decodeFromJsonElement(AppSettings.serializer(), json.parseToJsonElement(content))
        } catch (_: IllegalArgumentException) {
            AppSettings()
        } catch (_: SerializationException) {
            AppSettings()
        }
    }

}
