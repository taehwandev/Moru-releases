package app.thdev.moru.feature.workbench.beside

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruStatusGlyph
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruPaneHeader
import app.thdev.moru.feature.workbench.glyphKind
import app.thdev.moru.feature.workbench.i18n.workbenchStrings
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.feature.workbench.slots.TerminalPaneSpec
import app.thdev.moru.feature.workbench.slots.WorkbenchSessionSlots
import app.thdev.moru.feature.workbench.slots.WorkbenchTerminalSlots

/**
 * Another Task's terminals beside the selected Task, under a header with the Task's state, its title (clicking it
 * switches to that Task through [onSelect]) and a close button. A Task that needs the user tints its header so it is
 * seen without looking at the sidebar. The header takes [headerModifier] (its drag handle) and is tinted while
 * another Task's header would land on it.
 */
@Composable
internal fun BesideTaskPane(
    task: BesideTask,
    spec: TerminalPaneSpec,
    slots: WorkbenchTerminalSlots,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit = {},
    /** Answers one session's live permission request; see [WorkbenchSessionSlots.permissionActions]. */
    permissionActions: @Composable (sessionId: String, modifier: Modifier) -> Unit = { _, _ -> },
    headerModifier: Modifier = Modifier,
    dropHere: Boolean = false,
    /** The Task's terminal area with its session chooser; none draws [spec] through [slots] alone. */
    terminals: (@Composable (Modifier) -> Unit)? = null,
) {
    val strings = workbenchStrings()
    val state = strings.statusLabel(task.state)
    val needsYou = task.state.glyphKind() == MoruStatusGlyphKind.NeedsYou
    Column(modifier.ckSurface(MoruSurfaceRole.Terminal)) {
        Row(
            headerModifier.fillMaxWidth().height(MoruPaneHeader.height).ckSurface(MoruSurfaceRole.Card)
                .background(
                    when {
                        dropHere -> MoruTheme.colors.primary.copy(alpha = .18f)
                        needsYou -> MoruTheme.colors.warning.copy(alpha = .16f)
                        else -> Color.Transparent
                    },
                )
                .padding(horizontal = MoruPaneHeader.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruStatusGlyph(task.state.glyphKind(), contentDescription = state)
            MoruText(
                text = task.title,
                modifier = Modifier.weight(1f).clickable(role = Role.Button, onClickLabel = strings.switchToTask, onClick = onSelect),
                style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = MoruTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MoruText(
                text = state,
                style = MoruTheme.typography.caption,
                color = if (needsYou) MoruTheme.colors.warning else MoruTheme.colors.textSecondary,
                maxLines = 1,
            )
            MoruIconButton(onClick = onClose, description = strings.closeBeside, size = 20.dp) {
                MoruIcon(name = MoruIconName.Close, size = 12.dp, tint = MoruTheme.colors.textSecondary)
            }
        }
        // A request from one of this Task's terminals is answered here, without switching to the Task.
        val requests by SessionPermissions.requests.collectAsState()
        val asking = spec.sessionManager.listSessions()
            .filter { session -> session.spec.attachedItemId == task.id && session.id in requests }
        asking.forEach { session ->
            Box(
                Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Card)
                    .background(MoruTheme.colors.warning.copy(alpha = .10f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) { permissionActions(session.id, Modifier.fillMaxWidth()) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (terminals != null) terminals(Modifier.fillMaxSize()) else slots.pane(spec, Modifier.fillMaxSize())
        }
    }
}
