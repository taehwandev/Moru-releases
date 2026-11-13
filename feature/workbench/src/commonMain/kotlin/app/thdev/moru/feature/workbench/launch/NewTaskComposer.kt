package app.thdev.moru.feature.workbench.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * One step: pick what to start (Claude, Codex, AGY or a plain terminal) and that terminal opens at once. The
 * request is typed in the terminal, so there is no description box and no separate start button. The Task always
 * runs in the open project's folder. The host owns starting, errors and the Project/Task context.
 */
@Composable
fun NewTaskComposer(
    onStart: (runtime: AgentRuntime) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    lastRuntime: AgentRuntime? = null,
    isStarting: Boolean = false,
    errorMessage: String? = null,
) {
    Box(
        modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page).padding(MoruTheme.spacing.xLarge),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.large),
        ) {
            TaskComposerHeading()
            TaskComposerAgentChoice(
                lastRuntime = lastRuntime,
                onStart = { runtime -> if (!isStarting) onStart(runtime) },
                enabled = !isStarting,
            )
            TaskComposerActions(isStarting, errorMessage, onCancel)
        }
    }
}
