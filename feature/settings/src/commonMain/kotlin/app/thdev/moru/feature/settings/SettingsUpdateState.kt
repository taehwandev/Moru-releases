package app.thdev.moru.feature.settings

/** App-owned update information; Settings never checks releases or installs updates itself. */
data class SettingsUpdateState(
    val currentVersion: String? = null,
    val message: String? = null,
    val action: SettingsUpdateAction = SettingsUpdateAction.Unavailable,
    val failed: Boolean = false,
)
