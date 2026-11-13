package app.thdev.moru.core.model.preview

/** A box drawn over the page around an element that came from [file]; [x], [y], [width] and [height] are CSS pixels. */
data class PageOutline(
    val file: String,
    val component: String?,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)
