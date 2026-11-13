package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun MoruPaneDivider(
    label: String,
    resetLabel: String,
    onAdjust: (Dp) -> Unit,
    onDragDelta: (Dp) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier,
    overlay: Boolean = false,
    /** A seam between stacked panes: it drags up and down and answers the up and down arrows. */
    vertical: Boolean = false,
) {
    val density = LocalDensity.current
    val latestAdjust by rememberUpdatedState(onAdjust)
    val latestDrag by rememberUpdatedState(onDragDelta)
    val latestReset by rememberUpdatedState(onReset)
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val back = if (vertical) Key.DirectionUp else Key.DirectionLeft
    val forward = if (vertical) Key.DirectionDown else Key.DirectionRight
    Box(
        modifier
            .pointerHoverIcon(if (vertical) verticalResizePointerIcon() else horizontalResizePointerIcon())
            .hoverable(interactions)
            .background(
                if (hovered || focused) MoruTheme.colors.primary.copy(alpha = .12f)
                else if (overlay) Color.Transparent else MoruTheme.colors.background,
            )
            .semantics {
                contentDescription = label
                onClick(label = resetLabel) { latestReset(); true }
            }
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                    back -> { latestAdjust(-24.dp); true }
                    forward -> { latestAdjust(24.dp); true }
                    Key.Enter, Key.Home -> { latestReset(); true }
                    else -> false
                }
            }
            .focusable(interactionSource = interactions)
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { latestReset() }) }
            .draggable(
                state = rememberDraggableState { pixels -> latestDrag(with(density) { pixels.toDp() }) },
                orientation = if (vertical) Orientation.Vertical else Orientation.Horizontal,
                interactionSource = interactions,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(if (vertical) 36.dp else 2.dp, if (vertical) 2.dp else 36.dp)
                .background(if (hovered || focused) MoruTheme.colors.primary else MoruTheme.colors.border, RoundedCornerShape(2.dp)),
        )
        if (overlay) NativeDividerTarget(
            onDragDelta = { latestDrag(it) }, onReset = { latestReset() }, Modifier.fillMaxSize(), vertical = vertical,
        )
    }
}
