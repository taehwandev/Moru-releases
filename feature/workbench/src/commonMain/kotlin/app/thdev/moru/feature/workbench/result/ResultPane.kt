package app.thdev.moru.feature.workbench.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruPaneHeader
import app.thdev.moru.feature.workbench.i18n.resultStrings

/**
 * The Result of one Task: the [selected] tool's name over a single content area, so what the agent produced (code,
 * page, device screens) lives in one place instead of panels that pile up beside the terminal. Tools are chosen from
 * the strip beside it, which also opens and folds it. Stateless: the host owns the selection and placement. A detached
 * window offers Dock.
 */
@Composable
fun ResultPane(
    selected: ResultTab,
    modifier: Modifier = Modifier,
    placementAction: ResultPanePlacementAction? = null,
    /** Which Task's Result is shown, when there is more than one to choose from. */
    taskPicker: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val strings = resultStrings()
    // The glass sits on the terminal's backdrop, as the session tabs do, so both sides of the seam share one tone
    // instead of the Result turning lighter over the page behind it.
    Column(modifier = modifier.fillMaxSize().background(MoruTheme.colors.background).ckSurface(MoruSurfaceRole.Panel)) {
        // The same height and surface as the terminal's session tabs beside it, so the two header bands and the line
        // under them run straight across the seam.
        Row(
            Modifier.fillMaxWidth().height(MoruPaneHeader.height).ckSurface(MoruSurfaceRole.Panel).padding(start = MoruPaneHeader.horizontalPadding, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruText(
                strings.tab(selected),
                modifier = Modifier.weight(1f),
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
            taskPicker?.invoke()
            if (placementAction != null) {
                val docking = placementAction is ResultPanePlacementAction.Dock
                MoruIconButton(
                    onClick = placementAction.onClick,
                    description = if (docking) strings.dockResult else strings.detachResult,
                    size = 26.dp,
                ) {
                    MoruIcon(name = if (docking) MoruIconName.Dock else MoruIconName.Detach)
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

/** The height of the terminal session tabs, which the Result header lines up with. */
