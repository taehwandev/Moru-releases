package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Once per launch, reattaches every restored terminal whose program the terminal host still holds, so the sidebar
 * shows what each agent is doing without its Task being opened first. A terminal the host no longer holds is left to
 * start when its Task is shown, so a restart still never launches every agent at once. A program the host holds that
 * no restored terminal names (its Task went away, or its key was lost) has no screen left to reach it and would run on
 * while Moru stays connected, so it is ended.
 */
@Composable
internal fun ReattachHeldTerminals(
    sessions: TerminalSessionManager,
    bindings: MoruWorkbenchBindings,
    settings: AppSettings,
    colors: MoruColors,
) {
    LaunchedEffect(sessions) {
        val held = withContext(Dispatchers.IO) { bindings.heldTerminalKeys() } ?: return@LaunchedEffect
        heldTerminalSessions(sessions, held).forEach { session ->
            runCatching { bindings.startTerminal(session, settings, colors) }
        }
        unownedHeldKeys(sessions, held).forEach { key -> runCatching { bindings.terminateHeldTerminal(key) } }
    }
}

/** The keys in [held] that no terminal session of [sessions] names. */
internal fun unownedHeldKeys(sessions: TerminalSessionManager, held: Set<String>): Set<String> =
    held - sessions.listSessions().mapNotNull { session -> session.spec.hostKey }.toSet()

/** The terminal sessions whose host key is in [held]. */
internal fun heldTerminalSessions(sessions: TerminalSessionManager, held: Set<String>) =
    sessions.listSessions().filter { session ->
        session.spec.kind == TerminalSessionKind.TERMINAL && session.spec.hostKey?.let { it in held } == true
    }
