package app.thdev.moru.feature.workbench

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry

/**
 * How wide the explorer's rows must be for the longest visible name to show whole: its indent, chevron, icon, name and
 * change letter. The tree scrolls sideways when this is wider than the pane. Only the rows most likely to be widest are
 * measured, so a large open tree costs a handful of text layouts.
 */
@Composable
internal fun rememberProjectTreeContentWidth(rows: List<WorkbenchProjectTreeEntry>, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(rows, style, density) {
        rows
            .sortedByDescending { row -> row.name.length + row.depth * CharsPerIndent }
            .take(MeasuredRows)
            .maxOfOrNull { row ->
                val name = with(density) { measurer.measure(row.name, style, maxLines = 1, softWrap = false).size.width.toDp() }
                TreeStart + TreeIndent * row.depth + RowChrome + name
            } ?: 0.dp
    }
}

/** Chevron, icon, gaps, the name's lead, a change letter and the row's end padding. */
private val RowChrome = 12.dp + 4.dp + 14.dp + 4.dp + 2.dp + 4.dp + 12.dp + 10.dp

/** About how many characters one indent step is worth when guessing the widest rows. */
private const val CharsPerIndent = 2

/** Rows measured for the width: enough that a proportional font cannot hide the widest one. */
private const val MeasuredRows = 24
