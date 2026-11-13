package app.thdev.moru.core.data

import app.thdev.moru.core.model.AppTheme
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okio.Path.Companion.toOkioPath

class AppleStyleSettingsTest {
    @Test
    fun oldSettingsDefaultToStandardAndAppleStyleRoundTripsWithoutChangingOtherPreferences() {
        val directory = Files.createTempDirectory("apple-style-settings")
        try {
            val path = directory.resolve("settings.json")
            path.writeText("""{"appearance":{"theme":"tokyo_night","ide_font_size":18},"window":{"width_dp":1600}}""")
            val repository = FileBackedSettingsRepository(path.toOkioPath())
            val original = repository.loadSettings()
            assertFalse(original.appearance.appleStyle)
            repository.updateSettings { it.copy(appearance = it.appearance.copy(appleStyle = true)) }
            val reloaded = FileBackedSettingsRepository(path.toOkioPath()).loadSettings()
            assertTrue(reloaded.appearance.appleStyle)
            assertEquals(original.copy(appearance = original.appearance.copy(appleStyle = true)), reloaded)
            assertTrue(path.readText().contains("\"apple_style\": true"))
            repository.updateSettings { it.copy(appearance = it.appearance.copy(theme = AppTheme.Light)) }
            assertTrue(repository.loadSettings().appearance.appleStyle)
            repository.updateSettings { it.copy(appearance = it.appearance.copy(appleStyle = false)) }
            assertEquals(AppTheme.Light, repository.loadSettings().appearance.theme)
            assertFalse(repository.loadSettings().appearance.appleStyle)
            assertEquals(18, repository.loadSettings().appearance.uiFontSize)
            assertEquals(1600, repository.loadSettings().window.widthDp)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
