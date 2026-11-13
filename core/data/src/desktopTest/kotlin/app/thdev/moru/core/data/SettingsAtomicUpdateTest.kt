package app.thdev.moru.core.data

import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

class SettingsAtomicUpdateTest {
    @Test
    fun concurrentPreferenceAndWindowUpdatesPreserveBoth() = withSettingsPath { path ->
        val repo = FileBackedSettingsRepository(path)
        repo.saveSettings(AppSettings())
        val pool = Executors.newFixedThreadPool(2)
        try {
            val preferences = pool.submit {
                repeat(80) { repo.updateSettings { it.copy(terminal = it.terminal.copy(scrollbackLines = it.terminal.scrollbackLines + 1)) } }
            }
            val window = pool.submit {
                repeat(80) { repo.updateSettings { it.copy(window = it.window.copy(widthDp = it.window.widthDp + 1)) } }
            }
            preferences.get(10, TimeUnit.SECONDS)
            window.get(10, TimeUnit.SECONDS)
            assertEquals(5080, repo.loadSettings().terminal.scrollbackLines)
            assertEquals(1520, repo.loadSettings().window.widthDp)
            assertFalse(hasPendingWrites(path))
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun separateRepositoriesForTheSameFileDoNotLoseEachOthersUpdates() = withSettingsPath { path ->
        FileBackedSettingsRepository(path).saveSettings(AppSettings())
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first = pool.submit {
                val repo = FileBackedSettingsRepository(path)
                repeat(60) { repo.updateSettings { it.copy(terminal = it.terminal.copy(scrollbackLines = it.terminal.scrollbackLines + 1)) } }
            }
            val second = pool.submit {
                val repo = FileBackedSettingsRepository(path)
                repeat(60) { repo.updateSettings { it.copy(window = it.window.copy(widthDp = it.window.widthDp + 1)) } }
            }
            first.get(10, TimeUnit.SECONDS)
            second.get(10, TimeUnit.SECONDS)
            val stored = FileBackedSettingsRepository(path).loadSettings()
            assertEquals(5060, stored.terminal.scrollbackLines)
            assertEquals(1500, stored.window.widthDp)
            assertFalse(hasPendingWrites(path))
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun anUpdateRefusesToReplaceACorruptFileWithDefaults() = withSettingsPath { path ->
        val corrupt = "{ \"appearance\": { \"theme\": \"Dark\" ,,, }"
        FileSystem.SYSTEM.write(path) { writeUtf8(corrupt) }
        val repo = FileBackedSettingsRepository(path)
        assertFailsWith<IllegalStateException> {
            repo.updateSettings { it.copy(appearance = it.appearance.copy(theme = AppTheme.Light)) }
        }
        assertEquals(corrupt, FileSystem.SYSTEM.read(path) { readUtf8() })
    }

    @Test
    fun failedReplacementPreservesPreviousFileAndCleansPendingWrite() = withSettingsPath { path ->
        val original = AppSettings()
        FileBackedSettingsRepository(path).saveSettings(original)
        val failing = object : ForwardingFileSystem(FileSystem.SYSTEM) {
            override fun atomicMove(source: Path, target: Path) { throw IllegalStateException("Replacement denied") }
        }
        val repo = FileBackedSettingsRepository(path, failing)
        assertFailsWith<IllegalStateException> {
            repo.updateSettings { it.copy(appearance = it.appearance.copy(theme = AppTheme.Dark)) }
        }
        assertEquals(original, FileBackedSettingsRepository(path).loadSettings())
        assertFalse(hasPendingWrites(path))
    }

    @Test
    fun aNoOpUpdateDoesNotCreateSettingsOnDisk() = withSettingsPath { path ->
        val repo = FileBackedSettingsRepository(path)
        assertEquals(AppSettings(), repo.updateSettings { it })
        assertFalse(FileSystem.SYSTEM.exists(path))
    }

    private fun hasPendingWrites(path: Path): Boolean =
        FileSystem.SYSTEM.list(path.parent!!).any { it.name.endsWith(".saving") }

    private fun withSettingsPath(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("moru-settings-update")
        try { block(directory.resolve("settings.json").toOkioPath()) }
        finally { directory.toFile().deleteRecursively() }
    }
}
