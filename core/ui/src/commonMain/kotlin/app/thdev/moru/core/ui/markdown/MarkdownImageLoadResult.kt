package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.graphics.ImageBitmap

/** Decoded image and resolved open target, or a bounded unavailable result. */
sealed interface MarkdownImageLoadResult {
    data class Available(val bitmap: ImageBitmap, val openTarget: String) : MarkdownImageLoadResult
    data object Unavailable : MarkdownImageLoadResult
}
