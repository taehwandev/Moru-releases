package app.thdev.moru.feature.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Request identity matters: selecting the same session again must still return the keyboard.
internal class TerminalFocusRequest(val sessionKey: String)

internal var terminalFocusRequest by mutableStateOf<TerminalFocusRequest?>(null)
    private set

/** Requests keyboard focus after an explicit session choice, once its selected view is mounted. */
fun requestTerminalFocus(sessionKey: String) {
    terminalFocusRequest = TerminalFocusRequest(sessionKey)
}
