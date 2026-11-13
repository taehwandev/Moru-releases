package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitRepositorySnapshot

/**
 * Where the branch picked beside it syncs and how far apart they are. The branch itself is in the picker, so it is
 * not repeated; the folder stays in semantics only and a repository root shows only when it differs from the folder.
 */
@Composable
internal fun GitRepositoryHeader(snapshot: GitRepositorySnapshot?, workspacePath: String, modifier: Modifier = Modifier) {
    val path = workspacePath.ifBlank { "No folder selected" }
    Box(modifier.testTag("git-root-path").semantics { text = AnnotatedString(path) }) {
        if (snapshot == null) return@Box
        val ahead = snapshot.ahead
        val behind = snapshot.behind
        val counts = if (ahead != null && behind != null) " · ↑${formatGitCount(ahead)} ↓${formatGitCount(behind)}" else ""
        val root = if (snapshot.rootPath != workspacePath) " · Repository: ${snapshot.rootPath}" else ""
        MoruText(
            "${snapshot.upstream?.let { "Syncs with $it" } ?: "No upstream"}$counts$root",
            Modifier.testTag("git-branch-summary"),
            style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
