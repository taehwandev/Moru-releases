package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruBadge
import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

@Composable
actual fun RealTerminalContainer(
    modifier: Modifier,
    sessionKey: String,
    workingDirectory: String,
    shell: String,
    startupCommand: String?,
    onCommandCaptured: (String) -> Unit,
    fontFamily: String?,
    fontSize: Int,
    scrollbackLines: Int,
    onOutput: (atMillis: Long) -> Unit,
    onExit: (exitCode: Int?) -> Unit,
    focused: Boolean,
    hostKey: String?,
    onFocus: () -> Unit,
) {
    val colors = MoruTheme.colors
    val profile = LocalTerminalProfile.current
    val optionSendsEscape = LocalTerminalOptionSendsEscape.current
    val terminalPalette = TerminalPalette.from(colors, profile)
    // The PTY is keyed by the session only: a font or layout change must not restart a running
    // agent, and leaving the screen must not close it (see JediTermSessionRegistry).
    val terminalResult = remember(sessionKey) {
        runCatching {
            acquireDesktopTerminalSession(
                sessionKey = sessionKey,
                workingDirectory = workingDirectory,
                shell = shell,
                startupCommand = startupCommand,
                onCommandCaptured = onCommandCaptured,
                fontFamily = fontFamily,
                fontSize = fontSize,
                scrollbackLines = scrollbackLines,
                onOutput = onOutput,
                onExit = onExit,
                colors = colors,
                hostKey = hostKey,
                profile = profile,
                optionSendsEscape = optionSendsEscape,
            )
        }
    }
    val entry = terminalResult.getOrNull()
    val terminal = entry?.session
    if (entry != null) {
        // Hand the live terminal to this composition's callbacks and the current theme.
        entry.callbacks.onCommandCaptured = onCommandCaptured
        entry.callbacks.onOutput = onOutput
        entry.callbacks.onExit = onExit
        entry.paletteHolder.palette = terminalPalette
    }

    if (terminal == null) {
        TerminalStartupFailure(
            shell = shell,
            workingDirectory = workingDirectory,
            error = terminalResult.exceptionOrNull(),
            modifier = modifier,
        )
        return
    }

    val currentOnFocus by rememberUpdatedState(onFocus)
    DisposableEffect(terminal) {
        // Swing consumes these events before the Compose pane's clickable sees them.
        val targets = terminal.focusTargets()
        val mouse = object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) { currentOnFocus() }
        }
        val focus = object : FocusAdapter() {
            override fun focusGained(event: FocusEvent) {
                // Closing a popover or a queued focus request may return to an old pane after a new session was
                // chosen. Only user traversal chooses a pane here; mouse presses already report their choice.
                when (event.cause) {
                    FocusEvent.Cause.TRAVERSAL_FORWARD, FocusEvent.Cause.TRAVERSAL_BACKWARD,
                    FocusEvent.Cause.TRAVERSAL_UP, FocusEvent.Cause.TRAVERSAL_DOWN -> currentOnFocus()
                    else -> Unit
                }
            }
        }
        targets.forEach { target ->
            target.addMouseListener(mouse)
            target.addFocusListener(focus)
        }
        onDispose {
            targets.forEach { target ->
                target.removeMouseListener(mouse)
                target.removeFocusListener(focus)
            }
        }
    }

    // A selection change moves the keyboard, never a redraw: the screen redraws every
    // few seconds while agents run, and taking focus then would break off a Korean syllable being composed and send
    // the next keys to another terminal.
    LaunchedEffect(sessionKey, focused) {
        if (focused && !terminal.focusTarget().isFocusOwner) terminal.requestKeyboard()
    }
    // A click is a new request even when selection did not change. Keep it separate from redraws
    // and ignore requests for other terminals, so background work cannot take the keyboard.
    val focusRequest = terminalFocusRequest
    LaunchedEffect(sessionKey, focusRequest) {
        if (focused && focusRequest?.sessionKey == sessionKey) terminal.requestKeyboard()
    }
    // SwingPanel retains its factory component. Remount the host for another session;
    // the registry keeps the original PTY and buffer alive while its view is absent.
    key(sessionKey) {
        SwingPanel(
            modifier = modifier,
            background = terminalPalette.canvasComposeColor(),
            factory = { terminal.component() },
            update = {
                terminal.applyProfile(profile)
                terminal.applyOptionKey(optionSendsEscape)
                terminal.applyTheme()
                terminal.applyFont(fontFamily, fontSize)
            },
        )
    }
}

@Composable
private fun TerminalStartupFailure(
    shell: String,
    workingDirectory: String,
    error: Throwable?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .ckSurface(MoruSurfaceRole.Terminal)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MoruBadge(text = "terminal failed", type = MoruBadgeType.ERROR)
        MoruText(
            text = "Unable to start terminal session.",
            color = MoruTheme.colors.textPrimary,
            style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
        )
        MoruText(
            text = "shell=$shell",
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
        )
        MoruText(
            text = "cwd=$workingDirectory",
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
        )
        MoruText(
            text = error?.message?.takeIf { message -> message.isNotBlank() } ?: "No error detail was provided.",
            color = MoruTheme.colors.error,
            style = MoruTheme.typography.caption,
        )
    }
}
