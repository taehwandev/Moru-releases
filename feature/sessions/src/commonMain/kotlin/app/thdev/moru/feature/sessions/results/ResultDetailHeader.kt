package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.component.MoruStatusLabel
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.i18n.coreUiStrings
import app.thdev.moru.core.ui.time.formatRelativeTime
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * What the result is about: its Project, the Task as the page's title, its state, which agent and when, and the
 * evidence of what changed. Open goes to the session; Done ([onSeen], null once seen or for anything unfinished) files a finished result
 * under the ones already seen.
 */
@Composable
internal fun ResultDetailHeader(item: ResultItem, changedFiles: Int?, onOpen: () -> Unit, onSeen: (() -> Unit)?) {
    val strings = sessionsStrings()
    val row = item.row
    val ago = row.lastOutputAtMillis?.let { formatRelativeTime(coreUiStrings(), row.nowMillis / 1000, it / 1000) }
    val (status, glyph) = when (item.kind) {
        ResultKind.Done -> strings.resultsStatusDone to MoruStatusGlyphKind.Done
        ResultKind.Failed -> strings.resultsFailed to MoruStatusGlyphKind.Error
        ResultKind.Permission, ResultKind.Question -> strings.resultsStatusAsking to MoruStatusGlyphKind.NeedsYou
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = MoruTheme.spacing.xLarge, vertical = MoruTheme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.large),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
            MoruText(item.task?.projectName ?: strings.unattachedGroup, style = MoruTheme.typography.caption,
                color = MoruTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MoruText(item.task?.taskTitle ?: row.summary ?: row.title, style = MoruTheme.typography.h2,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small), verticalAlignment = Alignment.CenterVertically) {
                MoruStatusLabel(status, glyph)
                MoruText(listOfNotNull(strings.agentName(row.agent), ago).joinToString(" · "),
                    style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1)
            }
            ResultEvidence(row.sessionId, item.text, changedFiles)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
            MoruButton(strings.resultsOpen, onClick = onOpen, type = MoruButtonType.GHOST, size = MoruButtonSize.Small,
                modifier = Modifier.testTag("result-open:${row.sessionId}"))
            if (onSeen != null) MoruButton(strings.resultsMarkSeen, onClick = onSeen,
                type = MoruButtonType.SECONDARY, size = MoruButtonSize.Small, icon = MoruIconName.Check,
                modifier = Modifier.testTag("result-seen:${row.sessionId}"))
        }
    }
}
