package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/** Only this grip starts a drag; a tap focuses it without selecting or opening the row. */
@Composable
internal fun SidebarDragHandle(state: SidebarDragState, item: SidebarDragItem, description: String) {
    val focus = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val layout = remember { SidebarDragHandleLayout() }
    val strings = workbenchStrings()
    var hovered by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val dragging = state.isSource(item)
    val active = hovered || focused || dragging
    val gripAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.72f,
        animationSpec = tween(durationMillis = 140),
    )
    val baseColor = if (dragging || focused) MoruTheme.colors.primary else MoruTheme.colors.textSecondary
    val color = baseColor.copy(alpha = baseColor.alpha * gripAlpha)
    Canvas(
        Modifier.size(width = 12.dp, height = 20.dp)
            .testTag("sidebar-drag:${item.scope.projectId ?: "projects"}:${item.id}")
            .focusRequester(focus)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                when {
                    event.type != KeyEventType.KeyDown -> false
                    event.key == Key.Escape && state.isDragging -> { state.cancel(); true }
                    event.isAltPressed && event.key == Key.DirectionUp -> { state.move(item, -1); true }
                    event.isAltPressed && event.key == Key.DirectionDown -> { state.move(item, 1); true }
                    else -> false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { focus.requestFocus() },
            )
            .semantics {
                contentDescription = description
                customActions = listOf(
                    CustomAccessibilityAction(strings.moveSidebarItemUp) { state.move(item, -1) },
                    CustomAccessibilityAction(strings.moveSidebarItemDown) { state.move(item, 1) },
                )
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .onGloballyPositioned { layout.coordinates = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit -> hovered = false
                        }
                    }
                }
            }
            .pointerInput(state, item) {
                detectDragGestures(
                    onDragStart = { point ->
                        layout.toRoot(point)?.let { state.start(item, it); focus.requestFocus() }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        layout.toRoot(change.position)?.let(state::drag)
                    },
                    onDragEnd = state::finish,
                    onDragCancel = state::cancel,
                )
            },
    ) {
        if (gripAlpha > 0.01f) {
            for (column in 0..1) for (row in 0..2) {
                drawCircle(color, radius = 1.dp.toPx(), center = Offset(center.x + (column * 3.5f - 1.75f).dp.toPx(), center.y + (row * 4 - 4).dp.toPx()))
            }
        }
    }
}

private class SidebarDragHandleLayout {
    var coordinates: LayoutCoordinates? = null
    fun toRoot(point: Offset): Offset? = coordinates?.takeIf { it.isAttached }?.localToRoot(point)
}
