package app.thdev.moru.core.ui.search

/** The text a reader actually draws, excluding Markdown punctuation and link destinations. */
data class TextSearchLeaf(val path: String, val text: String) {
    fun matches(query: String): List<TextSearchMatch> {
        if (query.isEmpty()) return emptyList()
        return buildList {
            var from = 0
            while (from <= text.length - query.length) {
                val start = text.indexOf(query, from, ignoreCase = true)
                if (start < 0) break
                add(TextSearchMatch(path, start, start + query.length))
                from = start + query.length
            }
        }
    }
}
