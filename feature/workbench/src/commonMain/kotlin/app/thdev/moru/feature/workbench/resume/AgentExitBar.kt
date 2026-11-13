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
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/**
 * Said when an agent quit in a terminal that is still open: one button to start it again where it was, in that same
 * terminal, and one to leave it. Stateless.
 */
@Composable
fun AgentExitBar(
    exit: AgentExit,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = workbenchStrings()
    Row(
        modifier = modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Card).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruText(strings.agentExited(exit.runtime.displayName), modifier = Modifier.weight(1f), style = MoruTheme.typography.body)
        if (exit.resumeCommand != null) MoruButton(text = strings.resumeHere, onClick = onResume)
        MoruButton(text = strings.dismiss, onClick = onDismiss, type = MoruButtonType.GHOST)
    }
}
