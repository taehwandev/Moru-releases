package app.thdev.moru.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings

@Composable
fun rememberSettingsEditorState(
    settingsRepository: SettingsRepository,
    onSettingsSaved: (AppSettings) -> Unit = {},
): SettingsEditorState {
    val scope = rememberCoroutineScope()
    val savedCallback by rememberUpdatedState(onSettingsSaved)
    return remember(settingsRepository) {
        SettingsEditorState(settingsRepository, scope, onSettingsSaved = { savedCallback(it) })
    }
}
