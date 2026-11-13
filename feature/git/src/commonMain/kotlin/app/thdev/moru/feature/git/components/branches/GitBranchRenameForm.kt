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

/** The current ref is displayed rather than retyped; only its replacement name is editable. */
@Composable
internal fun GitBranchRenameForm(
    currentName: String,
    name: String,
    onNameChange: (String) -> Unit,
    enabled: Boolean,
    canRename: Boolean,
    onRename: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
        MoruText("Rename $currentName", maxLines = 2, overflow = TextOverflow.Ellipsis)
        MoruTextField(name, onNameChange, Modifier.fillMaxWidth().testTag("git-rename-name"), label = "New branch name", enabled = enabled)
        MoruButton("Rename branch", onRename, Modifier.fillMaxWidth().testTag("git-rename-branch"), MoruButtonType.PRIMARY, enabled && canRename)
        MoruButton("Cancel", onCancel, Modifier.fillMaxWidth().testTag("git-rename-cancel"), MoruButtonType.GHOST)
    }
}
