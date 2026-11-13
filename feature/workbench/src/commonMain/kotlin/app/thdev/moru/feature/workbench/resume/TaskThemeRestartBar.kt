package app.thdev.moru.feature.workbench.resume

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.terminal.SessionThemeRestarts
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.workbench.launch.AgentConversation
import app.thdev.moru.core.terminal.deriveSessionActivity
import kotlin.time.Clock

/** Restart progress belongs to the terminal and remains available after navigation. */
@Composable
internal fun TaskThemeRestartBar(
    session: TerminalSessionSnapshot,
    taskKey: String,
    sessions: TerminalSessionManager,
    nowMillis: Long,
    submitLine: (String, String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val state = SessionThemeRestarts.state(session.id)
    ThemeRestartBar(
        session = session,
        activity = deriveSessionActivity(session, nowMillis),
        inProgress = state == SessionThemeRestarts.State.Running,
        failed = state == SessionThemeRestarts.State.Failed,
        onRestart = {
            val spec = session.spec
            val command = AgentConversation.resumeCommandFor(spec.startupCommand)
            if (command != null) SessionThemeRestarts.request(
                session.id, "codex", command,
                canRestart = {
                    sessions.listSessions().firstOrNull { it.id == session.id }?.let { current ->
                        current.spec == spec &&
                            themeRestartGuard(current, taskKey, session.id, Clock.System.now().toEpochMilliseconds())
                    } == true
                },
                submitLine = submitLine,
            )
        },
        modifier = modifier,
    )
}
