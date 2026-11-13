package app.thdev.moru.app

/** Where Moru's own update stands, as the update banner shows it. */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class UpToDate(val version: String) : UpdateStatus
    data class Downloading(val version: String, val percent: Int) : UpdateStatus

    /** Downloaded and checked; restarting installs it. */
    data class Ready(val version: String) : UpdateStatus
    data class Installing(val version: String) : UpdateStatus
    data class Failed(val message: String) : UpdateStatus
}
