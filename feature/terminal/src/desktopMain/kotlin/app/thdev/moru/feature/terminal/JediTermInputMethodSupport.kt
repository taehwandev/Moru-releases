package app.thdev.moru.feature.terminal

import com.jediterm.terminal.ui.JediTermWidget

internal fun JediTermWidget.configureMoruInputMethodSupport() {
    enableInputMethods(true)
    focusTraversalKeysEnabled = false
    terminalPanel.apply {
        enableInputMethods(true)
        isFocusable = true
        focusTraversalKeysEnabled = false
    }
    preferredFocusableComponent.apply {
        enableInputMethods(true)
        focusTraversalKeysEnabled = false
    }
}
