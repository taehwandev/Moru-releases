package app.thdev.moru.feature.workbench.statusbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.path.LocalHomeDirectory
import app.thdev.moru.core.ui.path.homeRelativePath

/** Only current folder data is supplied by the app; full values remain accessible when labels truncate. */
@Composable
internal fun StatusBarProjectContext(folder: String, branch: String?, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (branch != null) {
            MoruIcon(MoruIconName.Branch, size = 12.dp, tint = MoruTheme.colors.textSecondary)
            MoruText(branch, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
                modifier = Modifier.widthIn(max = 160.dp).semantics { contentDescription = "Branch $branch" },
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MoruText(homeRelativePath(folder, LocalHomeDirectory.current), style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textSecondary, modifier = Modifier.weight(1f).semantics { contentDescription = "Path $folder" },
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
