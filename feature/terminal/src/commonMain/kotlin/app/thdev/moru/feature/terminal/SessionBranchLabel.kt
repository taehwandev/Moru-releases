package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** The branch a terminal works on, in its header, so each terminal says which checkout it is changing. */
@Composable
internal fun SessionBranchLabel(branch: String, modifier: Modifier = Modifier) {
    Row(
        modifier.widthIn(max = 220.dp).clearAndSetSemantics { contentDescription = "Branch $branch" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruIcon(MoruIconName.Branch, size = 12.dp, tint = MoruTheme.colors.textSecondary)
        MoruText(
            text = branch,
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
