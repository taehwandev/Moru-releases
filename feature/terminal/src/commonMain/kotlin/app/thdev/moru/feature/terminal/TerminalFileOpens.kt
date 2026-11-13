package app.thdev.moru.feature.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The file a terminal asked to show, until the app shows it and calls [clear]. */
object TerminalFileOpens {
    var request by mutableStateOf<TerminalFileOpenRequest?>(null)
        private set

    /** Asks the app to show [path], clicked in the terminal behind [sessionKey]. */
    fun open(sessionKey: String, path: String) {
        request = TerminalFileOpenRequest(sessionKey, path)
    }

    fun clear() {
        request = null
    }
}
