package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalWindowInfo
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.feature.workbench.restore.savedTerminalsOf
import kotlinx.coroutines.delay

/**
 * Copies what the workbench has open into [keeper] as it changes: the Projects the user collapsed, when [keepsCollapsed],
 * and every terminal with the one chosen. Terminal output does not move the manager's revision, so the terminals are
 * read on a short timer. Every window runs it, since a terminal opened in a Project window is one of the app's too.
 */
@Composable
internal fun KeepWorkbenchLayout(
    keeper: WorkbenchLayoutKeeper,
    navigation: WorkbenchNavigation,
    sessions: TerminalSessionManager,
    keepsCollapsed: Boolean,
) {
    LaunchedEffect(keeper, navigation.sidebar, keepsCollapsed) {
        if (keepsCollapsed) keeper.update { saved -> saved.copy(collapsedProjectIds = navigation.collapsedProjects().sorted()) }
    }
    // The terminals change through what the user does in a window, so each window reads them while it has focus and
    // once more as it loses it.
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(keeper, sessions, focused) {
        while (true) {
            val live = sessions.listSessions()
            val chosen = live.indexOfFirst { session -> session.id == sessions.selectedSessionId() }.takeIf { it >= 0 }
            keeper.update { saved -> saved.copy(terminals = savedTerminalsOf(live), selectedTerminal = chosen) }
            if (!focused) break
            delay(TerminalReadMillis)
        }
    }
}

private const val TerminalReadMillis = 1_000L
