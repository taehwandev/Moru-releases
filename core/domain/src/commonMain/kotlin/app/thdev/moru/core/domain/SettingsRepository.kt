package app.thdev.moru.core.domain

import app.thdev.moru.core.model.AppSettings

interface SettingsRepository {
    fun loadSettings(): AppSettings

    fun saveSettings(settings: AppSettings)

    /** Read/modify/write against the latest settings; persistent adapters serialize this operation. */
    fun updateSettings(transform: (AppSettings) -> AppSettings): AppSettings {
        val latest = loadSettings()
        val updated = transform(latest)
        if (updated != latest) saveSettings(updated)
        return updated
    }
}
