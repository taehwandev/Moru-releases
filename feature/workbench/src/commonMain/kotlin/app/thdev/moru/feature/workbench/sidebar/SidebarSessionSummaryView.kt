package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.thdev.moru.core.designsystem.component.MoruStatusLabel
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/** Wrapping labels keep simultaneous running and finished responses legible in a narrow sidebar. */
@Composable
internal fun SidebarSessionSummaryView(summary: SidebarSessionSummary, modifier: Modifier = Modifier) {
    val strings = workbenchStrings()
    if (summary.total == 0) {
        MoruText(strings.taskSessions(0, 0, 0), modifier, style = MoruTheme.typography.micro,
            color = MoruTheme.colors.textSecondary.copy(alpha = 0.74f), maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        return
    }
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
    ) {
        MoruText(strings.taskSessions(summary.total, 0, 0), style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textSecondary)
        if (summary.needsAttention > 0) {
            MoruStatusLabel(strings.sessionNeedsAttention(summary.needsAttention), MoruStatusGlyphKind.NeedsYou)
        }
        if (summary.running > 0) {
            MoruStatusLabel(strings.sessionRunning(summary.running), MoruStatusGlyphKind.Working)
        }
        if (summary.awaitingReply > 0) {
            MoruStatusLabel(strings.sessionAwaitingReply(summary.awaitingReply), MoruStatusGlyphKind.NeedsYou)
        }
        if (summary.responsesReady > 0) {
            MoruStatusLabel(strings.sessionResponsesReady(summary.responsesReady), MoruStatusGlyphKind.Done)
        }
        if (summary.waiting > 0) {
            MoruStatusLabel(strings.sessionWaiting(summary.waiting), MoruStatusGlyphKind.Idle)
        }
        if (summary.ended > 0) {
            MoruStatusLabel(strings.sessionEnded(summary.ended), MoruStatusGlyphKind.Idle)
        }
    }
}
