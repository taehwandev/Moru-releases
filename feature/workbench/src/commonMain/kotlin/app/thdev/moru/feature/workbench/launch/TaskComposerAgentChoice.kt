package app.thdev.moru.feature.workbench.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * What to start: one button per agent and one for a plain terminal. Pressing one opens that terminal at once;
 * the request is typed there, so there is nothing else to fill in first. The agent used last is drawn as the main
 * button when there is one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskComposerAgentChoice(
    lastRuntime: AgentRuntime?,
    onStart: (AgentRuntime) -> Unit,
    enabled: Boolean,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
    ) {
        AgentRuntime.entries.forEach { runtime ->
            MoruButton(
                text = runtime.displayName,
                onClick = { onStart(runtime) },
                // Once one was used before, it stands out and the rest step back; nothing is chosen for the user.
                type = if (lastRuntime == null || runtime == lastRuntime) MoruButtonType.PRIMARY else MoruButtonType.SECONDARY,
                enabled = enabled,
            )
        }
    }
}
