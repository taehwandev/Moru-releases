package app.thdev.moru.core.model.preview

import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Where a page in a [WebViewport] sits in a box of [boxWidth] x [boxHeight] pixels at [density] pixels per dp: its CSS
 * size, the device pixel ratio to lay it out with, and the centred rectangle it is drawn in. A fixed viewport is
 * never drawn larger than one dp per CSS pixel and shrinks to fit a smaller box ([shrink] below 1).
 */
data class BrowserLayout(
    val cssWidth: Int,
    val cssHeight: Int,
    val pixelRatio: Double,
    val mobile: Boolean,
    val shrink: Float,
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    companion object {
        fun of(viewport: WebViewport, boxWidth: Int, boxHeight: Int, density: Float): BrowserLayout? {
            if (boxWidth <= 0 || boxHeight <= 0 || density <= 0f) return null
            if (viewport == WebViewport.Fit) {
                val cssWidth = floor(boxWidth / density).toInt().coerceAtLeast(1)
                val cssHeight = floor(boxHeight / density).toInt().coerceAtLeast(1)
                return BrowserLayout(cssWidth, cssHeight, density.toDouble(), false, 1f, 0, 0, boxWidth, boxHeight)
            }
            val shrink = min(1f, min(boxWidth / density / viewport.width, boxHeight / density / viewport.height))
            val width = (viewport.width * density * shrink).roundToInt().coerceAtLeast(1)
            val height = (viewport.height * density * shrink).roundToInt().coerceAtLeast(1)
            return BrowserLayout(
                cssWidth = viewport.width,
                cssHeight = viewport.height,
                pixelRatio = viewport.scale,
                mobile = viewport.mobile,
                shrink = shrink,
                left = (boxWidth - width) / 2,
                top = (boxHeight - height) / 2,
                width = width,
                height = height,
            )
        }
    }
}
