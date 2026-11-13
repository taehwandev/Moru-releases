package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.layout.HorizontalAlignmentLine

/**
 * The center of the first rendered text line, in pixels relative to the layout exposing it.
 * Containers propagate the earliest text line; content without text leaves the line unspecified.
 */
val MarkdownFirstLineCenter = HorizontalAlignmentLine { first, second -> minOf(first, second) }
