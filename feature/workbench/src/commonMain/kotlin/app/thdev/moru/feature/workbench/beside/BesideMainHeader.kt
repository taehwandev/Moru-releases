package app.thdev.moru.feature.workbench.beside

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruStatusGlyph
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruPaneHeader
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.glyphKind
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/**
 * The selected Task's header while a Task is open beside it: the same row the Task beside has, naming the Task, tinted
 * as the selected one and without a close button; more strongly while the Task beside would land on it.
 */
@Composable
internal fun BesideMainHeader(title: String, state: TaskDisplayState?, modifier: Modifier = Modifier, dropHere: Boolean = false) {
    val strings = workbenchStrings()
    Row(
        modifier.fillMaxWidth().height(MoruPaneHeader.height).ckSurface(MoruSurfaceRole.Card)
            .background(MoruTheme.colors.primary.copy(alpha = if (dropHere) .18f else .10f))
            .padding(horizontal = MoruPaneHeader.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state?.let { MoruStatusGlyph(it.glyphKind(), contentDescription = strings.statusLabel(it)) }
        MoruText(
            text = title,
            modifier = Modifier.weight(1f),
            style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = MoruTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        state?.let {
            MoruText(text = strings.statusLabel(it), style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1)
        }
    }
}
