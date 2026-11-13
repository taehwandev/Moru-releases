package app.thdev.moru.app

import app.thdev.moru.app.i18n.AppStrings
import app.thdev.moru.feature.settings.SettingsUpdateAction
import app.thdev.moru.feature.settings.SettingsUpdateState

/** The menu, banner and Settings all reflect the same app-owned update. */
internal fun settingsUpdateState(
    strings: AppStrings,
    currentVersion: String?,
    status: UpdateStatus,
    checkAvailable: Boolean,
): SettingsUpdateState = SettingsUpdateState(
    currentVersion = currentVersion,
    message = updateMessage(strings, status),
    action = when {
        !checkAvailable -> SettingsUpdateAction.Unavailable
        status is UpdateStatus.Ready -> SettingsUpdateAction.Restart
        status is UpdateStatus.Checking || status is UpdateStatus.Downloading || status is UpdateStatus.Installing ->
            SettingsUpdateAction.Busy
        else -> SettingsUpdateAction.Check
    },
    failed = status is UpdateStatus.Failed,
)
