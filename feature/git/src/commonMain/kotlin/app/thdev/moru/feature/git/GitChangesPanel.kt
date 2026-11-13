package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruDivider
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruTextField
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitAction

/**
 * The changes to stage and commit, under two headings like a Git client: what is not staged yet, with Stage all, and
 * what the next commit takes, with Unstage all. A conflict stays under the first heading until it is resolved.
 */
@Composable
internal fun GitChangesPanel(
    state: GitManagementUiState,
    onEvent: (GitManagementEvent) -> Unit,
    modifier: Modifier = Modifier,
    onReviewFile: ((String) -> Unit)? = null,
    openPath: String? = null,
) {
    val snapshot = state.snapshot ?: return
    val staged = snapshot.files.filter { it.hasStaged && !it.isConflict }
    val unstaged = snapshot.files.filter { it.hasUnstaged || it.isConflict }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GitCommitTarget(snapshot)
        MoruDivider()
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("git-files"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (snapshot.files.isEmpty()) item { MoruText("Working folder is clean. No changes to stage.", color = MoruTheme.colors.textSecondary) }
            item(key = "unstaged-heading") {
                SectionHeading("Unstaged files", unstaged.size) {
                    MoruButton("Stage all", { onEvent(GitManagementEvent.ChangeIndex(GitAction.Stage(gitLiteralPaths(unstaged)), snapshot.revision)) },
                        Modifier.testTag("git-stage-all"), MoruButtonType.SECONDARY, state.interactive && unstaged.isNotEmpty(),
                        icon = MoruIconName.Plus, size = MoruButtonSize.Small)
                }
            }
            items(unstaged, key = { "unstaged:${it.path}" }) { file ->
                GitWorkingFileRow(file, staged = false, state.interactive, snapshot.revision, onEvent, onReviewFile, file.path == openPath)
            }
            item(key = "staged-heading") {
                SectionHeading("Staged files", staged.size, Modifier.padding(top = 8.dp)) {
                    MoruButton("Unstage all", { onEvent(GitManagementEvent.ChangeIndex(GitAction.Unstage(gitLiteralPaths(staged)), snapshot.revision)) },
                        Modifier.testTag("git-unstage-all"), MoruButtonType.SECONDARY, state.interactive && staged.isNotEmpty(),
                        icon = MoruIconName.Minus, size = MoruButtonSize.Small)
                }
            }
            items(staged, key = { "staged:${it.path}" }) { file ->
                GitWorkingFileRow(file, staged = true, state.interactive, snapshot.revision, onEvent, onReviewFile, file.path == openPath)
            }
        }
        MoruDivider()
        val summary = state.commitMessage.lineSequence().firstOrNull().orEmpty()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MoruText("Commit message", Modifier.weight(1f), style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold))
            // A Git summary line reads best under 72 characters; past it the count turns to a warning.
            MoruText("${summary.length}/72", style = MoruTheme.typography.caption,
                color = if (summary.length > 72) MoruTheme.colors.warning else MoruTheme.colors.textSecondary)
        }
        MoruTextField(state.commitMessage, { onEvent(GitManagementEvent.EditCommitMessage(it)) },
            Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 160.dp).testTag("git-commit-message")
                .semantics { contentDescription = "Commit message" },
            placeholder = "Summary, then a blank line and details", singleLine = false, enabled = state.interactive)
        MoruText(if (staged.size == 1) "Only the 1 staged file goes into this commit."
            else "Only the ${formatGitCount(staged.size)} staged files go into this commit.",
            style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        MoruButton(if (staged.isEmpty()) "Stage files to commit" else "Commit to ${gitBranchLabel(snapshot)}", { onEvent(GitManagementEvent.Propose(GitAction.Commit(state.commitMessage.trim()))) },
            Modifier.fillMaxWidth().testTag("git-commit"), enabled = state.interactive && staged.isNotEmpty() &&
                state.commitMessage.isNotBlank() && snapshot.files.none { it.isConflict } && snapshot.operationInProgress == null,
            icon = MoruIconName.Commit)
        if (snapshot.files.any { it.isConflict }) {
            MoruText("Conflicts are present. Resolve them before committing or changing branches.", style = MoruTheme.typography.caption, color = MoruTheme.colors.error)
        }
    }
}

@Composable
private fun SectionHeading(title: String, count: Int, modifier: Modifier = Modifier, action: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        MoruText("$title · ${formatGitCount(count)}", Modifier.weight(1f).semantics { heading() },
            style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
        action()
    }
}
