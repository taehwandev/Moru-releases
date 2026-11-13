package app.thdev.moru.feature.workbench.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.resultStrings

/**
 * The Result tools laid out one under another on the right edge, the way an IDE lists its tool windows. Each stays in
 * view whether the Result is open, folded or in its own window, so any tool is one click away. [onPick] decides
 * whether a click opens, switches or folds. While the Result is [detached], a Dock button brings it back.
 */
@Composable
internal fun ResultToolStrip(
    selected: ResultTab,
    open: Boolean,
    available: Set<ResultTab>,
    onPick: (ResultTab) -> Unit,
    modifier: Modifier = Modifier,
    detached: Boolean = false,
    onDock: () -> Unit = {},
) {
    val strings = resultStrings()
    Column(
        modifier.width(36.dp).fillMaxHeight().ckSurface(MoruSurfaceRole.Panel).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ResultToolButton(ResultTab.Code, active = open && selected == ResultTab.Code,
            hasContent = ResultTab.Code in available, onClick = { onPick(ResultTab.Code) })
        if (detached) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.semantics { contentDescription = strings.detachedResult }) {
                MoruIconButton(onClick = onDock, description = strings.dockResult, size = 28.dp) {
                    MoruIcon(MoruIconName.Dock)
                }
            }
        }
    }
}

@Composable
private fun ResultToolDivider(width: Dp = 18.dp) {
    Box(Modifier.padding(vertical = 4.dp).width(width).height(1.dp).background(MoruTheme.colors.border))
}
