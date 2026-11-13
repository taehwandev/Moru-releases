package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun GitApprovalPanel(
    approval: GitApproval,
    enabled: Boolean,
    onEvent: (GitManagementEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag("git-approval").ckSurface(MoruSurfaceRole.Card).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MoruText(approval.title, style = MoruTheme.typography.h2)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MoruText("Folder: ${approval.workspacePath}")
            if (approval.repositoryPath != approval.workspacePath) MoruText("Repository: ${approval.repositoryPath}")
            MoruText("Branch: ${approval.branch}")
            approval.details.forEach { MoruText(it) }
            MoruText("Git will recheck this repository before executing. Changed state requires a new approval.", style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoruButton("Cancel", { onEvent(GitManagementEvent.Cancel) }, Modifier.testTag("git-cancel"), MoruButtonType.SECONDARY, enabled)
            MoruButton("Confirm ${approval.title.lowercase()}", { onEvent(GitManagementEvent.Confirm) }, Modifier.testTag("git-confirm"), enabled = enabled)
        }
    }
}
