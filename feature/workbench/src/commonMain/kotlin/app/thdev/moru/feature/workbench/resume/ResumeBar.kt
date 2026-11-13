package app.thdev.moru.feature.workbench.resume

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings
import app.thdev.moru.feature.workbench.launch.AgentRuntime

/**
 * One tap to pick up where an agent left off: only agents with a known Task conversation, the Task's own agent first. It is
 * stateless; pressing one hands the agent to the host, which opens a new terminal for it.
 */
@Composable
fun ResumeBar(
    runtimes: List<AgentRuntime>,
    onResume: (AgentRuntime) -> Unit,
    modifier: Modifier = Modifier,
    preferred: AgentRuntime? = null,
) {
    val available = runtimes.distinct().filter { it.canResume }
    if (available.isEmpty()) return
    val strings = workbenchStrings()
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruText(strings.resumeLabel, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        // The agent this Task was started with comes first and stands out; the others stay one press away.
        available
            .sortedByDescending { runtime -> runtime == preferred }
            .forEach { runtime ->
                MoruButton(
                    text = runtime.displayName,
                    onClick = { onResume(runtime) },
                    type = if (runtime == preferred) MoruButtonType.PRIMARY else MoruButtonType.SECONDARY,
                )
            }
    }
}
