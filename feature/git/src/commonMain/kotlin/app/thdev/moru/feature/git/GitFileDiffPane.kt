package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.ui.diff.DiffFileView

/**
 * One file's diff in place of the commit history: its folder and name with what it is compared against, a close
 * button that brings the history back, and the lines. [file] is null while it loads or when [message] says why it
 * cannot be shown. The file list stays in the right column, so another file is one click away.
 */
@Composable
internal fun GitFileDiffPane(
    path: String,
    source: String,
    file: DiffFile?,
    message: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = path.substringAfterLast('/')
    val folder = path.substringBeforeLast('/', missingDelimiterValue = "")
    Column(modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page).testTag("git-file-diff")) {
        Row(
            Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Panel).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruIcon(MoruIconName.File, size = 14.dp, tint = MoruTheme.colors.textSecondary)
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (folder.isNotEmpty()) {
                    MoruText("$folder/", Modifier.weight(1f, fill = false), style = MoruTheme.typography.body,
                        color = MoruTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.StartEllipsis)
                }
                MoruText(name, Modifier.testTag("git-file-diff-path"),
                    style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
            }
            MoruText(source, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1)
            MoruIconButton(onClick = onClose, description = "Close diff and show history",
                modifier = Modifier.testTag("git-file-diff-close"), size = 32.dp) {
                MoruIcon(MoruIconName.Close)
            }
        }
        if (file != null) DiffFileView(file, Modifier.weight(1f).fillMaxWidth())
        else Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            MoruText(message, color = MoruTheme.colors.textSecondary)
        }
    }
}
