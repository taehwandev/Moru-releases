package app.thdev.moru.core.domain.markdown


/** Literal source search; matches never overlap and replacement strings are never interpreted. */
object MarkdownSearch {
    fun matches(text: String, query: String): List<MarkdownTextRange> {
        if (query.isEmpty()) return emptyList()
        val matches = mutableListOf<MarkdownTextRange>()
        var offset = 0
        while (offset <= text.length - query.length) {
            val start = text.indexOf(query, offset, ignoreCase = true)
            if (start < 0) break
            matches += MarkdownTextRange(start, start + query.length)
            offset = start + query.length
        }
        return matches
    }

    fun next(matches: List<MarkdownTextRange>, selection: MarkdownTextRange, backwards: Boolean = false): MarkdownTextRange? {
        if (matches.isEmpty()) return null
        val current = matches.indexOf(MarkdownTextRange(selection.min, selection.max))
        if (current >= 0) return matches[(current + if (backwards) matches.size - 1 else 1) % matches.size]
        return if (backwards) matches.lastOrNull { it.end <= selection.min } ?: matches.last()
        else matches.firstOrNull { it.start >= selection.max } ?: matches.first()
    }

    fun replaceCurrent(value: MarkdownEditValue, query: String, replacement: String): MarkdownEditValue {
        if (value.composition != null || query.isEmpty()) return value
        val selected = MarkdownTextRange(value.selection.min, value.selection.max)
        if (selected !in matches(value.text, query)) return value
        val text = value.text.replaceRange(selected.start, selected.end, replacement)
        val caret = selected.start + replacement.length
        val remaining = matches(text, query)
        val next = remaining.firstOrNull { it.start >= caret } ?: remaining.firstOrNull()
        return MarkdownEditValue(text, next ?: MarkdownTextRange(caret))
    }

    fun replaceAll(value: MarkdownEditValue, query: String, replacement: String): MarkdownEditValue {
        if (value.composition != null) return value
        val matches = matches(value.text, query)
        if (matches.isEmpty()) return value
        val text = buildString {
            var offset = 0
            for (match in matches) {
                append(value.text, offset, match.start)
                append(replacement)
                offset = match.end
            }
            append(value.text, offset, value.text.length)
        }
        return MarkdownEditValue(text, MarkdownTextRange(matches.first().start + replacement.length))
    }
}
