package app.thdev.moru.core.data

import app.thdev.moru.core.model.AppLanguage
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.AppearanceSettings
import app.thdev.moru.core.model.GeneralSettings
import app.thdev.moru.core.model.KeyboardSettings
import app.thdev.moru.core.model.ShortcutChord
import app.thdev.moru.core.model.TerminalSettings
import app.thdev.moru.core.model.WindowSettings
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okio.Path.Companion.toOkioPath

class FileBackedSettingsRepositoryTest {
    @Test
    fun loadSettingsReturnsDefaultsWhenFileIsMissing() {
        withTempDir { tempDir ->
            val repository = FileBackedSettingsRepository(
                tempDir.resolve(".moru-community/settings.json").toOkioPath(),
            )

            assertEquals(AppSettings(), repository.loadSettings())
        }
    }

    @Test
    fun saveAndLoadSettingsRoundTripsWorkspaceLocalFile() {
        withTempDir { tempDir ->
            val settingsPath = tempDir.resolve(".moru-community/settings.json")
            val repository = FileBackedSettingsRepository(settingsPath.toOkioPath())
            val settings = AppSettings(
                general = GeneralSettings(rememberLastState = false),
                appearance = AppearanceSettings(
                    theme = AppTheme.Dark,
                    terminalFontFamily = "JetBrains Mono",
                    terminalFontSize = 14,
                ),
                terminal = TerminalSettings(
                    shellCommand = "/bin/zsh",
                    scrollbackLines = 8000,
                ),
                window = WindowSettings(widthDp = 1680, heightDp = 1050, xDp = 120, yDp = 80),
            )

            repository.saveSettings(settings)

            assertEquals(settings, repository.loadSettings())
        }
    }

    @Test
    fun theChosenLanguagePersistsAndAnOlderFileWithoutOneFollowsTheSystem() {
        withTempDir { tempDir ->
            val settingsPath = tempDir.resolve(".moru-community/settings.json")
            val repository = FileBackedSettingsRepository(settingsPath.toOkioPath())

            repository.saveSettings(AppSettings(appearance = AppearanceSettings(language = AppLanguage.Korean)))
            assertEquals(AppLanguage.Korean, repository.loadSettings().appearance.language)
            assertTrue(settingsPath.readText().contains("\"ui_language\": \"ko\""))

            settingsPath.writeText("""{ "appearance": { "theme": "dark" } }""")
            assertEquals(AppLanguage.System, repository.loadSettings().appearance.language)
        }
    }

    @Test
    fun theRemovedGeneralLanguageKeyDoesNotChooseTheNewLanguage() {
        withTempDir { tempDir ->
            val settingsPath = tempDir.resolve(".moru-community/settings.json")
            settingsPath.parent.toFile().mkdirs()
            settingsPath.writeText(LEGACY_SETTINGS_JSON)
            val repository = FileBackedSettingsRepository(settingsPath.toOkioPath())

            // The legacy file says general.language is "ko"; that old key is dropped, not migrated.
            assertEquals(AppLanguage.System, repository.loadSettings().appearance.language)
        }
    }

    @Test
    fun changedAndRemovedShortcutsPersistAndAnOlderFileKeepsTheDefaults() {
        withTempDir { tempDir ->
            val settingsPath = tempDir.resolve(".moru-community/settings.json")
            val repository = FileBackedSettingsRepository(settingsPath.toOkioPath())
            val keyboard = KeyboardSettings()
                .assign(AppShortcut.ShowSettings, ShortcutChord("J", shift = true))
                .assign(AppShortcut.OpenProject9, null)

            repository.saveSettings(AppSettings(keyboard = keyboard))
            assertEquals(keyboard, repository.loadSettings().keyboard)

            settingsPath.writeText("""{ "appearance": { "theme": "dark" } }""")
            assertEquals(KeyboardSettings(), repository.loadSettings().keyboard)
        }
    }

    @Test
    fun loadSettingsIgnoresUnknownKeysAndKeepsDefaultsForMissingFields() {
        withTempDir { tempDir ->
            val settingsPath = tempDir.resolve(".moru-community/settings.json")
            settingsPath.parent.toFile().mkdirs()
            settingsPath.writeText(
                """
                {
                  "future_key": "ignored",
                  "appearance": {
                    "theme": "light",
                    "unknown_appearance_key": true
                  }
                }
                """.trimIndent(),
            )
            val repository = FileBackedSettingsRepository(settingsPath.toOkioPath())

            val settings = repository.loadSettings()

            assertEquals(AppTheme.Light, settings.appearance.theme)
            assertEquals(13, settings.appearance.terminalFontSize)
            assertEquals(5000, settings.terminal.scrollbackLines)
        }
    }

    @Test
    fun settingsFileWrittenBeforeDeadSettingsWereRemovedStillLoadsItsLiveValues() {
        withTempDir { tempDir ->
            val settingsPath = tempDir.resolve(".moru-community/settings.json")
            settingsPath.parent.toFile().mkdirs()
            settingsPath.writeText(LEGACY_SETTINGS_JSON)
            val repository = FileBackedSettingsRepository(settingsPath.toOkioPath())

            val settings = repository.loadSettings()

            assertEquals(AppTheme.TokyoNight, settings.appearance.theme)
            assertEquals("JetBrains Mono", settings.appearance.terminalFontFamily)
            assertEquals(15, settings.appearance.terminalFontSize)
            assertEquals("/bin/bash", settings.terminal.shellCommand)
            assertEquals(9000, settings.terminal.scrollbackLines)
            assertFalse(settings.general.rememberLastState)
            assertEquals(1500, settings.window.widthDp)
        }
    }

    @Test
    fun savingDropsRemovedSettingKeysFromAnOlderFile() {
        withTempDir { tempDir ->
            val settingsPath = tempDir.resolve(".moru-community/settings.json")
            settingsPath.parent.toFile().mkdirs()
            settingsPath.writeText(LEGACY_SETTINGS_JSON)
            val repository = FileBackedSettingsRepository(settingsPath.toOkioPath())

            val loaded = repository.loadSettings()
            repository.saveSettings(loaded)

            val saved = settingsPath.readText()
            listOf(
                "language",
                "notification_command",
                "density",
                "ui_font_family",
                "ui_font_size",
                "editor_font_family",
                "working_directory_policy",
                "default_provider",
                "max_steps",
                "command_palette",
                "mini_mode",
            ).forEach { removedKey ->
                assertFalse(saved.contains("\"$removedKey\""), "Expected `$removedKey` to be dropped on save")
            }
            assertEquals(loaded, repository.loadSettings(), "Live values must survive dropping the removed keys")
        }
    }

    @Test
    fun settingsAreSharedByEveryProjectAndTheOpenedProjectsOldOnesAreCarriedOverOnce() {
        withTempDir { tempDir ->
            val appData = tempDir.resolve("Moru").toFile()
            val first = tempDir.resolve("first").toFile().apply { resolve(".moru-community").mkdirs() }
            val second = tempDir.resolve("second").toFile().apply { resolve(".moru-community").mkdirs() }
            val dark = AppSettings(appearance = AppearanceSettings(theme = AppTheme.Dark))
            FileBackedSettingsRepository(first.resolve(".moru-community/settings.json").toPath().toOkioPath()).saveSettings(dark)
            FileBackedSettingsRepository(second.resolve(".moru-community/settings.json").toPath().toOkioPath())
                .saveSettings(AppSettings(appearance = AppearanceSettings(theme = AppTheme.Light)))

            // Opening the first Project carries its theme into the shared file.
            assertEquals(AppTheme.Dark, defaultDesktopSettingsRepository(appData, legacyProjectRoot = first).loadSettings().appearance.theme)
            // Any other Project now reads the same settings, never its own old file.
            val fromSecond = defaultDesktopSettingsRepository(appData, legacyProjectRoot = second)
            assertEquals(AppTheme.Dark, fromSecond.loadSettings().appearance.theme)
            fromSecond.saveSettings(dark.copy(appearance = AppearanceSettings(theme = AppTheme.TokyoNight)))
            assertEquals(AppTheme.TokyoNight, defaultDesktopSettingsRepository(appData, legacyProjectRoot = first).loadSettings().appearance.theme)
            assertTrue(appData.resolve("settings.json").isFile)
            assertTrue(first.resolve(".moru-community/settings.json").isFile, "old files are left where they were")
        }
    }

    private fun withTempDir(block: (java.nio.file.Path) -> Unit) {
        val tempDir = Files.createTempDirectory("moru-settings-test")
        try {
            block(tempDir)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    private companion object {
        /** A file as the app wrote it before the dead settings were removed, with every removed key present. */
        val LEGACY_SETTINGS_JSON = """
            {
              "schema_version": 1,
              "general": {
                "language": "ko",
                "remember_last_state": false,
                "notification_command": "say done"
              },
              "appearance": {
                "theme": "tokyo_night",
                "density": "compact",
                "ui_font_family": "Inter",
                "ui_font_size": 15,
                "terminal_font_family": "JetBrains Mono",
                "terminal_font_size": 15,
                "editor_font_family": "Fira Code",
                "editor_font_size": 14
              },
              "terminal": {
                "shell_command": "/bin/bash",
                "working_directory_policy": "last_terminal_cwd",
                "scrollback_lines": 9000
              },
              "runtime": { "default_provider": "claude", "default_model": "claude-4.5-sonnet" },
              "automation": { "max_steps": 20 },
              "shortcuts": { "command_palette": "Meta+P", "new_terminal": "Meta+Alt+T" },
              "window": { "width_dp": 1500, "height_dp": 950 },
              "mini_mode": { "enabled": true, "always_on_top": true }
            }
        """.trimIndent()
    }
}
