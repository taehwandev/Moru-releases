package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle

/**
 * How wide a table column wants to be, in pixels: [widest] holds its longest cell on one line, [narrowest] its longest
 * word, below which words would break.
 */
internal data class MarkdownTableColumn(val narrowest: Int, val widest: Int)

/**
 * Column widths for [available] pixels. A table that fits keeps each column to its content, so a number column stays
 * narrow; one that does not starts every column at its longest word and gives the rest of the room to the columns with
 * the most text, so long prose wraps and short values do not. Too narrow for whole words, long words break too: every
 * column starts at [floor] and the room is again shared by text, so prose keeps its width ahead of one long path. Only
 * a table that does not fit even at [floor] scrolls sideways.
 */
internal fun markdownTableWidths(columns: List<MarkdownTableColumn>, available: Int, floor: Int = 0): List<Int> =
    app.thdev.moru.core.domain.markdown.MarkdownTableLayout.widths(
        columns.map { app.thdev.moru.core.domain.markdown.MarkdownTableLayout.Column(it.narrowest,it.widest) }, available, floor,
    )

/**
 * Measures every column of [table] as it is drawn, code in its monospace face, adding [padding] for the cell's sides.
 * A single word longer than [longestWord] may still break, so one long path does not push the table off the screen.
 */
internal fun measureMarkdownTableColumns(
    table: MarkdownBlock.Table,
    measurer: TextMeasurer,
    body: TextStyle,
    header: TextStyle,
    padding: Int,
    longestWord: Int,
): List<MarkdownTableColumn> = table.header.indices.map { column ->
    var widest = 0
    var narrowest = 0
    (listOf(table.header[column] to header) + table.rows.map { it.getOrElse(column) { "" } to body }).forEach { (cell, style) ->
        val runs = markdownRuns(cell)
        widest = maxOf(widest, measurer.measure(cellText(runs), style, softWrap = false, maxLines = 1).size.width)
        runs.forEach { run ->
            run.text.split(Whitespace).filter(String::isNotEmpty).forEach { word ->
                val text = if (run.code) AnnotatedString(word, SpanStyle(fontFamily = FontFamily.Monospace)) else AnnotatedString(word)
                narrowest = maxOf(narrowest, measurer.measure(text, style, softWrap = false, maxLines = 1).size.width)
            }
        }
    }
    narrowest = narrowest.coerceAtMost(longestWord)
    MarkdownTableColumn(narrowest = minOf(narrowest, widest) + padding, widest = widest + padding)
}

private fun cellText(runs: List<MarkdownRun>): AnnotatedString = buildAnnotatedString {
    runs.forEach { run -> if (run.code) withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(run.text) } else append(run.text) }
}

private val Whitespace = Regex("\\s+")
