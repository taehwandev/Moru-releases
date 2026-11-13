package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun KnowledgeSourceRow(
    source: KnowledgeSourceCard,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (!source.found) 0.55f else 1f)
            .ckSurface(MoruSurfaceRole.Row)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
            KnowledgeSourceSymbol(source = source)
            MoruText(source.title, Modifier.weight(1f), style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold))
            MoruText(source.status, style = MoruTheme.typography.label, color = MoruTheme.colors.textSecondary)
        }
        MoruText(
            text = source.pathHint,
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.label.copy(fontFamily = FontFamily.Monospace),
        )
        if (source.capabilities.isNotEmpty()) MoruText(source.capabilities.joinToString(" / "), style = MoruTheme.typography.label)
        Row(horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) { actions() }
    }
}

@Composable
private fun KnowledgeSourceSymbol(source: KnowledgeSourceCard) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(MoruTheme.radius.xSmall))
            .background(MoruTheme.colors.primary.copy(alpha = 0.12f))
            .border(1.dp, MoruTheme.colors.primary.copy(alpha = 0.38f), RoundedCornerShape(MoruTheme.radius.xSmall)),
        contentAlignment = Alignment.Center,
    ) {
        MoruText(
            text = source.symbol,
            color = MoruTheme.colors.primary,
            style = MoruTheme.typography.monospace.copy(fontWeight = FontWeight.Bold),
        )
    }
}
