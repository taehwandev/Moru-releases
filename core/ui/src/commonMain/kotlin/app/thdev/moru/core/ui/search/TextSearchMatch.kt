package app.thdev.moru.core.ui.search

/** One literal occurrence in a uniquely named rendered text surface. Offsets are UTF-16 text offsets. */
data class TextSearchMatch(val path: String, val start: Int, val end: Int)
