package app.thdev.moru.feature.terminal

import app.thdev.moru.feature.terminal.search.TerminalFindController
import com.jediterm.terminal.model.StyleState
import com.jediterm.terminal.model.TerminalTextBuffer
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.TerminalPanel
import com.jediterm.terminal.ui.TerminalAction
import com.jediterm.terminal.ui.settings.SettingsProvider
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JScrollBar

internal class MoruJediTermWidget(
    columns: Int,
    rows: Int,
    settingsProvider: MoruTerminalSettingsProvider,
) : JediTermWidget(columns, rows, settingsProvider) {
    /** Every ask to give this terminal the keyboard, on the widget or its panel, so a test can catch an unwanted one. */
    val focusRequests = AtomicInteger()
    internal val findController by lazy {
        TerminalFindController(
            this, terminalPanel, terminalTextBuffer,
            restoreSelection = { (terminalPanel as MoruTerminalPanel).restoreSearchSelection(it) },
            findShortcuts = mySettingsProvider.findActionPresentation.keyStrokes,
        )
    }

    override fun getActions(): List<TerminalAction> = listOf(
        TerminalAction(mySettingsProvider.findActionPresentation) { findController.open(); true },
    )

    override fun requestFocusInWindow(): Boolean {
        focusRequests.incrementAndGet()
        return findController.keyboardTarget.requestFocusInWindow()
    }

    internal fun refreshFindAppearance() = findController.applyAppearance()

    override fun close() {
        findController.dispose()
        super.close()
    }

    override fun createTerminalPanel(
        settings: SettingsProvider,
        style: StyleState,
        buffer: TerminalTextBuffer,
    ): TerminalPanel = MoruTerminalPanel(
        settings, buffer, style,
        onFocusRequest = { focusRequests.incrementAndGet() },
        onFindKey = { findController.handleTerminalKey(it) },
    )

    override fun createScrollBar(): JScrollBar = JScrollBar().apply {
        isVisible = false
        isOpaque = false
        preferredSize = Dimension(0, 0)
        minimumSize = Dimension(0, 0)
        maximumSize = Dimension(0, Int.MAX_VALUE)
        unitIncrement = 0
        blockIncrement = 0
    }
}
