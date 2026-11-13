package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult

/**
 * Tells [onHover] which link of [text] is under the pointer, and null once it is over none or leaves. [layout] holds
 * the text's latest layout. The pointer is only watched, so selection and clicks work as before.
 */
internal fun Modifier.markdownLinkHover(
    text: AnnotatedString,
    layout: () -> TextLayoutResult?,
    onHover: (String?) -> Unit,
): Modifier = pointerInput(text, onHover) {
    var shown: String? = null
    try {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val target = if (event.type == PointerEventType.Exit) null
                else event.changes.firstOrNull()?.position?.let { layout()?.linkAt(it, text) }
                if (target != shown) {
                    shown = target
                    onHover(target)
                }
            }
        }
    } finally {
        // A link that scrolls away or is redrawn under a still pointer must not keep its destination showing.
        if (shown != null) onHover(null)
    }
}

/** The target of the link drawn at [position], read from the character the pointer is over, not the nearest one. */
internal fun TextLayoutResult.linkAt(position: Offset, text: AnnotatedString): String? {
    val offset = getOffsetForPosition(position)
    val character = listOf(offset, offset - 1).firstOrNull { it in text.indices && getBoundingBox(it).contains(position) }
        ?: return null
    return text.getLinkAnnotations(character, character + 1)
        .firstNotNullOfOrNull { (it.item as? LinkAnnotation.Clickable)?.tag }
}
