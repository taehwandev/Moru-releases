package app.thdev.moru.feature.codepanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuItem
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.feature.codepanel.i18n.codePanelStrings

/** Names the checkout the Code panel reads and lets the user read another checkout of the same repository. */
@Composable
internal fun WorktreePicker(
    worktrees: List<CodeWorktree>,
    shown: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = codePanelStrings()
    var expanded by remember { mutableStateOf(false) }
    fun nameOf(worktree: CodeWorktree) = if (worktree.isMain) strings.mainWorktree(worktree.label) else worktree.label
    val current = worktrees.firstOrNull { worktree -> worktree.path == shown }?.let(::nameOf).orEmpty()
    // With a single checkout there is nothing to pick, so it reads as a plain label, not a button that does nothing.
    val pickable = worktrees.size > 1
    Box(modifier) {
        Row(
            Modifier.height(24.dp)
                .then(if (pickable) Modifier.ckSurface(MoruSurfaceRole.Control).clickable(role = Role.Button) { expanded = true } else Modifier)
                .semantics { contentDescription = "${strings.worktree}: $current" }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MoruIcon(iconOf(worktrees.firstOrNull { worktree -> worktree.path == shown }), size = 12.dp)
            MoruText(
                current,
                style = MoruTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (pickable) MoruIcon(MoruIconName.ChevronDown, size = 12.dp)
        }
        MoruDropdownMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            items = worktrees.map { worktree ->
                val name = nameOf(worktree)
                MoruMenuItem(name, checked = worktree.path == shown, icon = iconOf(worktree)) { onPick(worktree.path) }
            },
        )
    }
}

/** The same marks as the Git branch picker: a house for the main checkout, trees for a linked worktree. */
private fun iconOf(worktree: CodeWorktree?): MoruIconName = when {
    worktree == null -> MoruIconName.Branch
    worktree.isMain -> MoruIconName.Home
    else -> MoruIconName.Worktree
}
