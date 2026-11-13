package app.thdev.moru.feature.git.components.branches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruTextField
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Names are requested only while creating a branch; the caller owns both source choices. */
@Composable
internal fun GitBranchCreateForm(
    name: String,
    onNameChange: (String) -> Unit,
    selectedName: String?,
    enabled: Boolean,
    hasHead: Boolean,
    onFromHead: () -> Unit,
    onFromSelected: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
        MoruTextField(name, onNameChange, Modifier.fillMaxWidth().testTag("git-create-name"), label = "New local branch name", enabled = enabled)
        MoruButton("Create from HEAD", onFromHead, Modifier.fillMaxWidth().testTag("git-create-branch"),
            MoruButtonType.PRIMARY, enabled && name.isNotBlank() && hasHead)
        if (selectedName != null) {
            MoruButton("Create from selected", onFromSelected, Modifier.fillMaxWidth().testTag("git-create-from-selected"),
                MoruButtonType.SECONDARY, enabled && name.isNotBlank())
            MoruText("Source: $selectedName", style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        MoruText("Creates without switching", style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        MoruButton("Cancel", onCancel, Modifier.fillMaxWidth().testTag("git-create-cancel"), MoruButtonType.GHOST)
    }
}
