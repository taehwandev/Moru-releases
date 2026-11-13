package app.thdev.moru.feature.codepanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.codepanel.i18n.codePanelStrings

/** A run of unchanged lines folded away between the changed ranges; the whole file is in the Code tab. */
@Composable
internal fun FoldedCodeRow(count: Int, modifier: Modifier = Modifier) {
    val colors = MoruTheme.colors
    Row(
        modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(colors.surface)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        MoruText("⋯", style = MoruTheme.typography.monospace, color = colors.textSecondary, modifier = Modifier.width(66.dp))
        MoruText(codePanelStrings().unchangedLines(count), style = MoruTheme.typography.caption, color = colors.textSecondary)
    }
}
