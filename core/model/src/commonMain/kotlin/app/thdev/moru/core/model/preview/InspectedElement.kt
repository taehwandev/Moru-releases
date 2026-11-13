package app.thdev.moru.core.model.preview

/**
 * What a click in Inspect mode found on the page: where the element is written ([file] relative to the dev server's
 * root, with [line] and [column] when the page knows them), the component it belongs to, and enough of the element
 * to recognise it. [text] is short and never holds what was typed into a field. Sizes are CSS pixels.
 */
data class InspectedElement(
    val file: String?,
    val line: Int?,
    val column: Int?,
    val component: String?,
    val tag: String,
    val text: String,
    val selector: String,
    val width: Int,
    val height: Int,
)
