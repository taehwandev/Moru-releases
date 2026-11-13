package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException

/**
 * Full-viewport image preview that scales the image to fill the available surface by default and supports zooming
 * (controls, scroll wheel, double-click) and drag panning while zoomed in.
 */
@Composable
fun MarkdownImagePreview(
    source: String,
    modifier: Modifier = Modifier,
    description: String = "Result image",
    imageBasePath: String? = null,
    imageLoader: MarkdownImageLoader? = null,
) {
    val loader = imageLoader ?: remember { defaultMarkdownImageLoader() }
    val result by produceState<MarkdownImageLoadResult?>(null, source, imageBasePath, loader) {
        value = null
        value = try {
            loader.load(source, imageBasePath)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            MarkdownImageLoadResult.Unavailable
        }
    }
    var scale by remember(source, imageBasePath) { mutableStateOf(MinZoom) }
    var offset by remember(source, imageBasePath) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    fun updateTransform(targetScale: Float, targetOffset: Offset = offset) {
        val clampedScale = targetScale.coerceIn(MinZoom, MaxZoom)
        val maxX = (viewportSize.width * (clampedScale - 1f)).coerceAtLeast(0f) / 2f
        val maxY = (viewportSize.height * (clampedScale - 1f)).coerceAtLeast(0f) / 2f
        scale = clampedScale
        offset = if (clampedScale <= MinZoom) {
            Offset.Zero
        } else {
            Offset(
                x = targetOffset.x.coerceIn(-maxX, maxX),
                y = targetOffset.y.coerceIn(-maxY, maxY),
            )
        }
    }

    DisableSelection {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(MoruTheme.radius.small))
                .ckSurface(MoruSurfaceRole.Code)
                .onSizeChanged { viewportSize = it }
                .testTag("markdown-image-preview"),
            contentAlignment = Alignment.Center,
        ) {
            when (val image = result) {
                is MarkdownImageLoadResult.Available -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(source, imageBasePath) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > MinZoom) updateTransform(MinZoom, Offset.Zero)
                                        else updateTransform(DoubleTapZoom, Offset.Zero)
                                    },
                                )
                            }
                            .pointerInput(source, imageBasePath) {
                                detectDragGestures { change, dragAmount ->
                                    if (scale > MinZoom) {
                                        change.consume()
                                        updateTransform(scale, offset + dragAmount)
                                    }
                                }
                            }
                            .pointerInput(source, imageBasePath) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Scroll) {
                                            val delta = event.changes.fold(0f) { acc, change -> acc - change.scrollDelta.y }
                                            if (delta != 0f) {
                                                val step = ( kotlin.math.abs(delta) * ZoomWheelStep ).coerceIn(0.05f, 0.5f)
                                                val next = if (delta > 0f) scale * (1f + step) else scale / (1f + step)
                                                updateTransform(next)
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = image.bitmap,
                            contentDescription = description.ifBlank { "Result image" },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .testTag("markdown-image"),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(MoruTheme.spacing.small)
                            .clip(RoundedCornerShape(MoruTheme.radius.medium))
                            .ckSurface(MoruSurfaceRole.Card)
                            .padding(horizontal = MoruTheme.spacing.xSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
                    ) {
                        MoruIconButton(
                            icon = MoruIconName.Minus,
                            description = "Zoom out",
                            onClick = { updateTransform(scale - ZoomButtonStep) },
                            modifier = Modifier.testTag("markdown-image-zoom-out"),
                            enabled = scale > MinZoom,
                            size = MoruButtonSize.Small,
                        )
                        MoruButton(
                            text = "${(scale * 100).roundToInt()}%",
                            onClick = { updateTransform(MinZoom, Offset.Zero) },
                            modifier = Modifier.testTag("markdown-image-zoom-reset"),
                            type = MoruButtonType.GHOST,
                            size = MoruButtonSize.Small,
                        )
                        MoruIconButton(
                            icon = MoruIconName.Plus,
                            description = "Zoom in",
                            onClick = { updateTransform(scale + ZoomButtonStep) },
                            modifier = Modifier.testTag("markdown-image-zoom-in"),
                            enabled = scale < MaxZoom,
                            size = MoruButtonSize.Small,
                        )
                    }
                }
                MarkdownImageLoadResult.Unavailable -> MoruText(
                    "Image unavailable",
                    modifier = Modifier.testTag("markdown-image-unavailable"),
                    style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary,
                )
                null -> MoruText(
                    "Loading image…",
                    modifier = Modifier.testTag("markdown-image-loading"),
                    style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary,
                )
            }
        }
    }
}

private const val MinZoom = 1f
private const val MaxZoom = 5f
private const val DoubleTapZoom = 2f
private const val ZoomButtonStep = 0.25f
private const val ZoomWheelStep = 0.15f
