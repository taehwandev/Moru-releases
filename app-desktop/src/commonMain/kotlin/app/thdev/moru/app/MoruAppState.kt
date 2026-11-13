package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings

/** Owns the initial preference read and settings already saved by the settings screen. */
class MoruAppState(repository: SettingsRepository) {
    var settings by mutableStateOf(repository.loadSettings())
        private set

    fun settingsSaved(settings: AppSettings) {
        this.settings = settings
    }
}
