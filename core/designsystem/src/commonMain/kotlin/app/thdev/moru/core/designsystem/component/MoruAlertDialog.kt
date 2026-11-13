package app.thdev.moru.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * A modal alert: the whole window dims behind a card in its middle, so the question has the user's attention. It is a
 * native popup window, so it dims and covers native views such as terminals too. A click on the dimmed window calls
 * [onDismiss]; [onKey] sees every key first (Escape and Enter are the caller's to decide). The card is [width] wide.
 */
@Composable
fun MoruAlertDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 400.dp,
    onKey: (KeyEvent) -> Boolean = { false },
    content: @Composable ColumnScope.() -> Unit,
) {
    val focus = remember { FocusRequester() }
    val window = rememberMoruPopoverWindowSize() ?: with(LocalDensity.current) {
        val size = LocalWindowInfo.current.containerSize
        androidx.compose.ui.unit.DpSize(size.width.toDp(), size.height.toDp())
    }
    Popup(
        popupPositionProvider = WindowOrigin,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        // The popup window keeps drawing frames while it reveals, so its first frame at the owner's size is replaced.
        val reveal = remember { Animatable(0f) }
        LaunchedEffect(Unit) { reveal.animateTo(1f, tween(RevealMillis)) }
        Box(
            Modifier
                .size(window)
                .graphicsLayer { alpha = reveal.value }
                .background(Scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .onPreviewKeyEvent(onKey)
                .focusRequester(focus)
                .focusable(),
            contentAlignment = Alignment.Center,
        ) {
            val shape = RoundedCornerShape(14.dp)
            Column(
                modifier
                    .width(width)
                    .graphicsLayer {
                        val scale = 0.96f + 0.04f * reveal.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(24.dp, shape)
                    .clip(shape)
                    .background(MoruTheme.colors.surface)
                    // A click on the card itself must not dismiss it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(22.dp),
                content = content,
            )
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}

/** Places the popup over the whole owner window. */
private object WindowOrigin : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
        IntOffset.Zero
}

private val Scrim = Color.Black.copy(alpha = 0.45f)
private const val RevealMillis = 140
