package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** A Results section's title in its state color, with how many results it holds. */
@Composable
internal fun ResultsSectionHeading(label: String, count: Int, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(top = MoruTheme.spacing.small, bottom = MoruTheme.spacing.xSmall),
        horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruText(label, Modifier.weight(1f), style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = color, maxLines = 1)
        MoruText(count.toString(), style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}
