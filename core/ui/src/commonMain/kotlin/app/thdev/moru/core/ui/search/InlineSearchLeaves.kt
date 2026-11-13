package app.thdev.moru.core.ui.search

import app.thdev.moru.core.ui.markdown.markdownRuns

/** Image nodes split text just as the inline reader does; image pixels and hidden destinations are not text. */
internal fun inlineSearchLeaves(markdown: String, path: String): List<TextSearchLeaf> {
    val runs = markdownRuns(markdown)
    if (runs.none { it.imageSource != null }) return listOf(TextSearchLeaf("$path/inline:0", runs.joinToString("") { it.text }))
    return buildList {
        var start = 0
        runs.forEachIndexed { index, run ->
            if (run.imageSource != null) {
                if (index > start) add(TextSearchLeaf("$path/inline:$start", runs.subList(start, index).joinToString("") { it.text }))
                start = index + 1
            }
        }
        if (start < runs.size) add(TextSearchLeaf("$path/inline:$start", runs.subList(start, runs.size).joinToString("") { it.text }))
    }
}
