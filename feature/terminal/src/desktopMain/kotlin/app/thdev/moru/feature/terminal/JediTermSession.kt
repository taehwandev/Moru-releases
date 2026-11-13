package app.thdev.moru.feature.terminal

import app.thdev.moru.core.model.terminal.TerminalProfile
import app.thdev.moru.core.terminal.TerminableTtyConnector
import app.thdev.moru.core.terminal.ThemeReportingTtyConnector
import app.thdev.moru.core.terminal.SessionPermissionPrompt
import app.thdev.moru.core.terminal.screenPermission
import app.thdev.moru.core.terminal.permissionInput
import app.thdev.moru.core.terminal.terminalScreenRows
import app.thdev.moru.core.terminal.connectingTtyConnector
import app.thdev.moru.core.terminal.createDesktopJediTermTtyConnector
import app.thdev.moru.core.terminal.desktopTerminalShellCommand
import javax.swing.JComponent
import javax.swing.SwingUtilities

internal class JediTermSession(
    shell: String,
    workingDirectory: String,
    startupCommand: String?,
    onCommandCaptured: (String) -> Unit,
    onOutput: () -> Unit,
    onExit: (Int?) -> Unit,
    onLocalServerUrl: (String) -> Unit,
    onOutputLine: (String) -> Unit,
    onScreenQuestion: (String?) -> Unit = {},
    onScreenBusy: (Boolean) -> Unit = {},
    onScreenPermission: (SessionPermissionPrompt?) -> Unit = {},
    onScreenReplyQuestion: (String?) -> Unit = {},
    onScreenLastReply: (String) -> Unit = {},
    onProcessStarted: (pid: Long) -> Unit,
    /** The terminal title the program sets, where Claude Code and Codex say what they are working on. */
    onTitle: (String) -> Unit = {},
    /** A file path the user clicked in the output, made absolute. */
    onOpenFile: (String) -> Unit = {},
    /** Moru attached, resized or typed into the program; its redraw that follows is not work. */
    onStimulus: () -> Unit = {},
    hostKey: String? = null,
    fontFamily: String?,
    fontSize: Int,
    scrollbackLines: Int,
    private val paletteHolder: TerminalPalette.Holder,
    profile: TerminalProfile? = null,
    optionSendsEscape: Boolean = true,
    private val onCanvasChanged: (rgb: Int) -> Unit = {},
    onBackgroundColorRead: (rgb: Int) -> Unit = {},
    /** Called, before [onExit], when the terminal could not be started or reached. */
    private val onStartFailed: () -> Unit = {},
    onInputSubmitted: () -> Unit = {},
) {
    private val settingsProvider = MoruTerminalSettingsProvider(
        fontFamily = fontFamily,
        fontSize = fontSize,
        scrollbackLines = scrollbackLines,
        paletteHolder = paletteHolder,
        profile = profile,
        optionSendsEscape = optionSendsEscape,
    )
    private val widget = MoruJediTermWidget(
        InitialColumns,
        InitialRows,
        settingsProvider,
    )
    private val screenObservation = TerminalScreenObservation(widget.terminalTextBuffer, onOutputLine, onScreenQuestion, onScreenBusy, onScreenPermission,
        onScreenReplyQuestion, onScreenLastReply)
    private var appliedPalette = paletteHolder.palette

    /** True once the terminal could not be started or reached; see [onStartFailed]. */
    @Volatile var failedToStart = false
        private set

    @Volatile private var closed = false

    init {
        try {
            widget.terminal.addApplicationTitleListener { title -> onTitle(title) }
            widget.addHyperlinkFilter(TerminalFileLinkFilter(workingDirectory, onOpenFile))
            paletteHolder.palette.applyTo(widget)
            widget.terminalPanel.setDefaultCursorShape(settingsProvider.cursorShape())
            recordCanvas()
            widget.configureMoruInputMethodSupport()
            val connect = {
                createDesktopJediTermTtyConnector(
                    shell = shell,
                    workingDirectory = workingDirectory,
                    startupCommand = startupCommand,
                    onCommandCaptured = onCommandCaptured,
                    columns = InitialColumns,
                    rows = InitialRows,
                    onOutput = onOutput,
                    onExit = { code -> screenObservation.close(); onExit(code) },
                    onLocalServerUrl = onLocalServerUrl,
                    // A terminal closed while it connected is already forgotten; it must not be recorded again.
                    onProcessStarted = { pid -> if (!closed) onProcessStarted(pid) },
                    onStimulus = { if (!closed) onStimulus() },
                    hostKey = hostKey,
                    onBackgroundColorRead = onBackgroundColorRead,
                    onInputSubmitted = { if (!closed) onInputSubmitted() },
                )
            }
            widget.setTtyConnector(
                // A terminal inside Moru starts at once, as before, and JediTerm sees its process connector. Starting or
                // reaching the terminal host can take seconds, so that runs off the UI thread; a missing folder or shell
                // still fails here first, so whoever opens the terminal can say it could not start.
                if (hostKey == null) connect() else {
                    checkTerminalCanStart(shell, workingDirectory)
                    connectingTtyConnector(connect) { error ->
                        System.err.println("Moru could not start a terminal in $workingDirectory: $error")
                        failedToStart = true
                        screenObservation.close()
                        if (!closed) {
                            onStartFailed()
                            onExit(null)
                        }
                    }
                },
            )
            widget.start()
        } catch (error: Throwable) {
            screenObservation.close()
            runCatching { widget.close() }
            throw error
        }
    }

    fun component(): JComponent {
        return widget.component
    }

    /** The part of the terminal that takes the keyboard and the input method. */
    fun focusTarget(): JComponent = widget.terminalPanel

    /** Native finder controls must select their pane on a click or keyboard traversal, just like its terminal. */
    internal fun focusTargets(): List<JComponent> = widget.findController.bar.let { bar ->
        listOf(widget.terminalPanel, bar.queryField, bar.previousButton, bar.nextButton, bar.closeButton)
    }

    /** How many times anything has asked to give this terminal the keyboard, so a test can see an unwanted ask. */
    internal val keyboardRequests get() = widget.focusRequests

    /** Gives this terminal the keyboard. Only a choice the user made may call it: moving focus breaks off an IME composition. */
    fun requestKeyboard() {
        SwingUtilities.invokeLater { widget.requestFocusInWindow() }
    }

    /**
     * Writes [text] to the process as typed input; false if the terminal is not connected. The write itself happens in
     * order on this terminal's own input thread: a program that is not reading its input (stopped, busy, its input
     * queue full) would otherwise hold whoever typed, the UI thread included, until it reads again.
     */
    fun type(text: String): Boolean =
        runCatching {
            val connector = widget.ttyConnector ?: return false
            if (!connector.isConnected) return false
            inputThread.value.execute { runCatching { connector.write(text) } }
            true
        }.getOrDefault(false)

    private val inputThread = lazy {
        java.util.concurrent.Executors.newSingleThreadExecutor { task -> Thread(task, "moru-terminal-input").apply { isDaemon = true } }
    }

    /** Pastes [text] into the program as a clipboard paste would; false if the terminal is not connected. */
    fun paste(text: String): Boolean =
        widget.ttyConnector?.isConnected == true && (widget.terminalPanel as? MoruTerminalPanel)?.paste(text) == true

    /**
     * Sends [text] as the user's next message: pasted, so a multi-line answer stays one message, then Enter on its own
     * a moment later, after the agent has taken the paste in.
     */
    fun submit(text: String): Boolean {
        if (!paste(text)) return false
        inputThread.value.execute {
            Thread.sleep(SubmitAfterPasteMillis)
            runCatching { widget.ttyConnector?.takeIf { it.isConnected }?.write("\r") }
        }
        return true
    }

    /** Checks the live screen again under its model lock before writing a fixed menu response. */
    fun answerPermission(expected: SessionPermissionPrompt, choiceId: String): Boolean {
        val buffer = widget.terminalTextBuffer
        if (!buffer.tryLock()) return false
        return try {
            if (screenPermission(terminalScreenRows(buffer)) != expected) false
            else permissionInput(expected, choiceId)?.let(::type) ?: false
        } finally {
            buffer.unlock()
        }
    }

    /** Applies a font changed in Settings to this running terminal; the program keeps running and gets the new size. */
    fun applyFont(fontFamily: String?, fontSize: Int) {
        if (!settingsProvider.updateFont(fontFamily, fontSize)) return
        (widget.terminalPanel as? MoruTerminalPanel)?.refreshFont()
    }

    /** Applies the Option key setting to this running terminal. */
    fun applyOptionKey(sendsEscape: Boolean) {
        settingsProvider.optionSendsEscape = sendsEscape
    }

    /** Applies an imported profile's spacing and cursor to this running terminal; its program keeps running. */
    fun applyProfile(profile: TerminalProfile?) {
        if (profile == settingsProvider.profile) return
        val relayout = settingsProvider.updateProfile(profile)
        widget.terminalPanel.setDefaultCursorShape(settingsProvider.cursorShape())
        if (relayout) (widget.terminalPanel as? MoruTerminalPanel)?.refreshFont()
    }

    /** The screen and history the terminal keeps, so a test can read what was drawn and how. */
    internal val textBuffer get() = widget.terminalTextBuffer

    /** The font size the terminal draws with now. */
    internal val fontSize: Int get() = settingsProvider.terminalFont.size

    /** How many columns fit the terminal now, which a bigger font makes fewer. */
    internal val columns: Int get() = widget.terminal.terminalWidth

    fun applyTheme() {
        val palette = paletteHolder.palette
        if (palette == appliedPalette) return
        val canvasChanged = palette.canvas != appliedPalette.canvas
        palette.applyTo(widget)
        appliedPalette = palette
        if (canvasChanged) recordCanvas()
        // A program that asked to hear theme changes (Claude Code's Auto theme) is told, so it redraws in the new one.
        if (canvasChanged) (widget.ttyConnector as? ThemeReportingTtyConnector)?.let { connector ->
            runCatching { connector.reportThemeChange(palette.isDark) }
        }
    }

    private fun recordCanvas() {
        val canvas = appliedPalette.canvas
        onCanvasChanged((canvas.red shl 16) or (canvas.green shl 8) or canvas.blue)
    }

    /**
     * Takes the terminal off screen. With [terminate] its program ends too (the user closed it); without, a program in
     * the terminal host keeps running for the next time Moru opens (Moru is quitting).
     */
    fun close(terminate: Boolean = true) {
        closed = true
        // What was typed still goes first, for a moment; nothing more is taken.
        if (inputThread.isInitialized()) inputThread.value.run {
            shutdown()
            runCatching { awaitTermination(InputDrainMillis, java.util.concurrent.TimeUnit.MILLISECONDS) }
        }
        runCatching { screenObservation.close() }
        runCatching {
            val connector = widget.ttyConnector
            if (terminate && connector is TerminableTtyConnector) connector.terminate() else connector.close()
        }
        runCatching {
            widget.close()
        }
    }
}

private const val InputDrainMillis = 300L
private const val SubmitAfterPasteMillis = 120L
private const val InitialColumns = 120
private const val InitialRows = 36

/** Throws when [shell] or [workingDirectory] cannot start a terminal, the failures a PTY reports at once. */
private fun checkTerminalCanStart(shell: String, workingDirectory: String) {
    require(java.io.File(workingDirectory).isDirectory) { "The folder $workingDirectory does not exist." }
    val program = desktopTerminalShellCommand(shell).first()
    if (program.startsWith("/")) require(java.io.File(program).canExecute()) { "The shell $program cannot be run." }
}
