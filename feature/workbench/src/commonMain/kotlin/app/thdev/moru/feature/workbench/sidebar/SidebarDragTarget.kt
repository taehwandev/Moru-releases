package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Measures the whole group, including Task terminals, without shifting its layout during a drag. */
@Composable
internal fun SidebarDragTarget(
    state: SidebarDragState,
    item: SidebarDragItem,
    content: @Composable () -> Unit,
) {
    DisposableEffect(state, item) { onDispose { state.removeBounds(item) } }
    val insertion = state.insertion?.takeIf { it.item == item }
    val color = MoruTheme.colors.primary
    Box(
        Modifier.fillMaxWidth()
            .testTag("sidebar-group:${item.scope.projectId ?: "projects"}:${item.id}")
            .onGloballyPositioned { coordinates ->
                val origin = coordinates.positionInRoot()
                state.setBounds(item, Rect(origin.x, origin.y, origin.x + coordinates.size.width, origin.y + coordinates.size.height))
            }
            .drawWithContent {
                drawContent()
                if (insertion != null) {
                    val y = if (insertion.after) size.height - 1.dp.toPx() else 1.dp.toPx()
                    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                }
            },
    ) { content() }
}
