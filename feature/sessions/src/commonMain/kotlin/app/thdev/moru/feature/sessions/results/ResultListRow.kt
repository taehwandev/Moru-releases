package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruStatusDot
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.ui.i18n.coreUiStrings
import app.thdev.moru.core.ui.time.formatRelativeTime
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * One result in the page's list: its Project and how long ago, the Task, and a line of what it says or asks, with a
 * dot in its state color. The chosen one sits on the selected surface.
 */
@Composable
internal fun ResultListRow(item: ResultItem, selected: Boolean, onSelect: () -> Unit) {
    val strings = sessionsStrings()
    val row = item.row
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(MoruTheme.radius.medium)
    val ago = row.lastOutputAtMillis?.let { formatRelativeTime(coreUiStrings(), row.nowMillis / 1000, it / 1000) }
    val preview = when (item.kind) {
        ResultKind.Done -> item.text?.let(::resultPreview)
        ResultKind.Failed -> row.lastLine ?: strings.resultsFailed
        else -> row.replyQuestion ?: row.question ?: item.text?.let(::resultPreview)
    }
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .then(when {
                selected -> Modifier.ckSurface(MoruSurfaceRole.RowSelected)
                hovered -> Modifier.background(MoruTheme.colors.textSecondary.copy(alpha = 0.06f))
                else -> Modifier
            })
            .hoverable(interaction).clickable(onClick = onSelect)
            .padding(horizontal = MoruTheme.spacing.small, vertical = MoruTheme.spacing.small)
            .testTag("result-row:${row.sessionId}"),
        horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        MoruStatusDot(item.kind.badge(), Modifier.padding(top = MoruTheme.spacing.xSmall))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoruText(item.task?.projectName ?: strings.unattachedGroup, Modifier.weight(1f),
                    style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                ago?.let { MoruText(it, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1) }
            }
            MoruText(item.task?.taskTitle ?: row.summary ?: row.title,
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            preview?.let {
                MoruText(it, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun ResultKind.badge(): MoruBadgeType = when (this) {
    ResultKind.Done -> MoruBadgeType.SUCCESS
    ResultKind.Failed -> MoruBadgeType.ERROR
    ResultKind.Permission, ResultKind.Question -> MoruBadgeType.WARNING
}
