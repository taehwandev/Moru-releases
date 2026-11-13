package app.thdev.moru.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.thdev.moru.core.designsystem.theme.MoruGlassRole

/**
 * A small panel that opens over the screen from the control that owns it and closes on an outside click or Escape.
 * Unlike [MoruDropdownMenu] it holds any content, so a list the user reads belongs here, not a list of commands.
 * Place it inside the anchor's `Box` so it opens beside that control. Its native window draws the shared
 * theme surface without embedding a Swing component into the foreground.
 * The caller owns its content insets; the popup adds no menu or shadow gutter. Long content scrolls within the
 * owner window. All hosts share the same readable theme surface.
 */
@Composable
fun MoruPopover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    /** Lines the popup's end up with the anchor's end, for a popup dropping from a button at a pane's end. */
    alignEnd: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val focus = remember { FocusRequester() }
    val ownerSize = rememberMoruPopoverWindowSize()
    // Native antialias drawing extends two dp beyond the content on each side.
    val drawingInset = if (ownerSize != null) 2.dp else 0.dp
    val drawingInsetPx = with(LocalDensity.current) { drawingInset.roundToPx() }
    val position = remember(drawingInsetPx, alignEnd) { MoruPopoverPositionProvider(drawingInsetPx, alignEnd) }
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = false),
    ) {
        // The native popup window opens at its owner's size and shrinks to the content after the first frame; a
        // panel drawn only once stays blank or retains its old canvas offset. Keep drawing frames after
        // viewport/owner size changes too, without replaying a full fade while the user resizes the window.
        val reveal = remember { Animatable(0f) }
        var viewportSize by remember { mutableStateOf(IntSize.Zero) }
        LaunchedEffect(viewportSize, ownerSize) {
            reveal.snapTo(reveal.value.coerceAtMost(0.99f))
            reveal.animateTo(1f, tween(PopoverRevealMillis))
        }
        // Keep the native popup's painted content within its measured viewport.
        Box(Modifier.sizeIn(
            maxWidth = ownerSize?.let { (it.width - drawingInset * 2).coerceAtLeast(0.dp) } ?: Dp.Unspecified,
            maxHeight = ownerSize?.let { (it.height - drawingInset * 2).coerceAtLeast(0.dp) } ?: Dp.Unspecified,
        ).onSizeChanged { viewportSize = it }.clipToBounds().graphicsLayer {
            alpha = reveal.value
            val scale = 0.97f + 0.03f * reveal.value
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }) {
            MoruPopoverMaterial {
                Column(
                    modifier
                        .moruGlassSurface(MoruGlassRole.Sheet, shadow = false)
                        .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                        .onPreviewKeyEvent { event ->
                            if (onPreviewKeyEvent(event)) true
                            else if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                                onDismiss()
                                true
                            } else false
                        }.focusRequester(focus).focusable(),
                    content = content,
                )
            }
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
        MoruPopoverDismissEvents(onDismiss, onPreviewKeyEvent)
    }
}

private const val PopoverRevealMillis = 140
