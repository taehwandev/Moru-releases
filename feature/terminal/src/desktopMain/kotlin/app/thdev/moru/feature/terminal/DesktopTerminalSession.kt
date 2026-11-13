package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.terminal.TerminalProfile
import app.thdev.moru.core.terminal.LocalServerUrls
import app.thdev.moru.core.terminal.SessionLastLines
import app.thdev.moru.core.terminal.SessionBusyScreens
import app.thdev.moru.core.terminal.SessionQuestions
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.terminal.SessionTitles
import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.SessionStimuli
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.terminal.SessionThemeColors
import app.thdev.moru.core.terminal.desktopTerminalShellCommand
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import javax.swing.SwingUtilities
import app.thdev.moru.core.terminal.SessionReplyQuestions
import app.thdev.moru.core.terminal.SessionResults

/** Starts the retained terminal immediately, even when its Compose view has not been mounted. */
fun startDesktopTerminalSession(
    sessionKey: String,
    workingDirectory: String,
    shell: String,
    startupCommand: String?,
    onCommandCaptured: (String) -> Unit,
    fontFamily: String?,
    fontSize: Int,
    scrollbackLines: Int,
    onOutput: (Long) -> Unit = {},
    onExit: (Int?) -> Unit = {},
    colors: MoruColors = lightCkColors,
    hostKey: String? = null,
    profile: TerminalProfile? = null,
    optionSendsEscape: Boolean = true,
) {
    acquireDesktopTerminalSession(
        sessionKey, workingDirectory, shell, startupCommand, onCommandCaptured,
        fontFamily, fontSize, scrollbackLines, onOutput, onExit, colors, hostKey, profile, optionSendsEscape,
    )
}

/** The launch path and the visible container share one registry entry and one PTY. */
internal fun acquireDesktopTerminalSession(
    sessionKey: String,
    workingDirectory: String,
    shell: String,
    startupCommand: String?,
    onCommandCaptured: (String) -> Unit,
    fontFamily: String?,
    fontSize: Int,
    scrollbackLines: Int,
    onOutput: (Long) -> Unit = {},
    onExit: (Int?) -> Unit = {},
    colors: MoruColors = lightCkColors,
    hostKey: String? = null,
    profile: TerminalProfile? = null,
    optionSendsEscape: Boolean = true,
): JediTermSessionRegistry.Entry = onSwingEventThread {
    val palette = TerminalPalette.from(colors, profile)
    val entry = JediTermSessionRegistry.acquire(
        key = sessionKey,
        initialPalette = palette,
        create = { callbacks, paletteHolder ->
            // The process may emit output or exit during construction, before any view exists.
            callbacks.bind(onCommandCaptured, onOutput, onExit)
            JediTermSession(
                shell = shell,
                workingDirectory = workingDirectory,
                startupCommand = startupCommand,
                onCommandCaptured = { callbacks.onCommandCaptured(it) },
                onOutput = { callbacks.onOutput(System.currentTimeMillis()) },
                onExit = { SessionPermissions.forget(sessionKey); callbacks.onExit(it) },
                onLocalServerUrl = { LocalServerUrls.record(sessionKey, it) },
                onOutputLine = { SessionLastLines.record(sessionKey, it) },
                onScreenQuestion = { SessionQuestions.record(sessionKey, it) },
                onScreenBusy = { SessionBusyScreens.record(sessionKey, it) },
                onScreenReplyQuestion = { SessionReplyQuestions.record(sessionKey, it) },
                onScreenLastReply = { SessionResults.recordScreen(sessionKey, it) },
                onScreenPermission = { prompt ->
                    SessionPermissions.observe(sessionKey, prompt) { choice ->
                        prompt != null && JediTermSessionRegistry.find(sessionKey)?.session?.answerPermission(prompt, choice) == true
                    }
                },
                onTitle = { SessionTitles.record(sessionKey, it) },
                onOpenFile = { TerminalFileOpens.open(sessionKey, it) },
                onProcessStarted = { SessionShellProcesses.record(sessionKey, it, desktopTerminalShellCommand(shell).first()) },
                onStimulus = { SessionStimuli.record(sessionKey, System.currentTimeMillis()) },
                onInputSubmitted = { SessionResponseAttention.inputSubmitted(sessionKey) },
                hostKey = hostKey,
                fontFamily = fontFamily,
                fontSize = fontSize,
                scrollbackLines = scrollbackLines,
                paletteHolder = paletteHolder,
                profile = profile,
                optionSendsEscape = optionSendsEscape,
                onCanvasChanged = { SessionThemeColors.recordCanvas(sessionKey, it) },
                onBackgroundColorRead = { SessionThemeColors.recordBackgroundColor(sessionKey, it) },
                // A terminal that could not start leaves its key free, so opening it again tries again.
                onStartFailed = { JediTermSessionRegistry.releaseIfFailed(sessionKey) },
            )
        },
    )
    entry.callbacks.bind(onCommandCaptured, onOutput, onExit)
    SessionResults.registerSender(sessionKey) { text -> JediTermSessionRegistry.find(sessionKey)?.session?.submit(text) == true }
    entry.paletteHolder.palette = palette
    entry.session.applyProfile(profile)
    entry.session.applyOptionKey(optionSendsEscape)
    entry
}

private fun JediTermSessionRegistry.Callbacks.bind(
    command: (String) -> Unit,
    output: (Long) -> Unit,
    exit: (Int?) -> Unit,
) {
    onCommandCaptured = command
    onOutput = output
    onExit = exit
}

private fun <T> onSwingEventThread(action: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return action()
    val result = CompletableFuture<T>()
    SwingUtilities.invokeLater {
        runCatching(action).onSuccess { result.complete(it) }.onFailure { result.completeExceptionally(it) }
    }
    // Join waits for the actual outcome even if the calling thread is interrupted: a launch
    // must not be reported as failed while an already queued EDT action starts its process.
    return try {
        result.join()
    } catch (error: CompletionException) {
        throw error.cause ?: error
    }
}
