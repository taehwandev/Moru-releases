package app.thdev.moru.app

import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.AppearanceSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class MoruAppStateTest {
    @Test
    fun savedSettingsUpdateStateWithoutRepeatingStorageReadsOrWrites() {
        var reads = 0
        var writes = 0
        val initial = AppSettings(appearance = AppearanceSettings(theme = AppTheme.Light))
        val repository = object : SettingsRepository {
            override fun loadSettings(): AppSettings { reads += 1; return initial }
            override fun saveSettings(settings: AppSettings) { writes += 1 }
        }
        val state = MoruAppState(repository)
        assertEquals(initial, state.settings)
        val saved = initial.copy(appearance = initial.appearance.copy(theme = AppTheme.Dark))

        state.settingsSaved(saved)

        assertEquals(saved, state.settings)
        assertEquals(1, reads)
        assertEquals(0, writes, "The settings screen already saved this action")
    }
}
