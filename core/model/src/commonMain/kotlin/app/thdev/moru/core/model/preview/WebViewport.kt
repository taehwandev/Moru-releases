package app.thdev.moru.core.model.preview

/**
 * The window a web Preview lays the page out in. [Phone], [Tablet] and [Desktop] keep their size whatever the pane's,
 * shrunk to fit when the pane is smaller, like a device frame; [Fit] follows the pane. Sizes are CSS pixels.
 */
enum class WebViewport(val width: Int, val height: Int, val scale: Double, val mobile: Boolean) {
    Phone(390, 844, 3.0, true),
    Tablet(820, 1180, 2.0, true),
    Desktop(1440, 900, 1.0, false),
    Fit(0, 0, 1.0, false),
    ;

    companion object {
        /** The saved name back to a viewport; unknown names (from an older or newer Moru) are dropped. */
        fun fromName(name: String): WebViewport? = entries.firstOrNull { it.name == name }
    }
}
