package app.thdev.moru.core.domain.markdown

/** Existing live syntax presentation in original UTF16 source coordinates. */
class MarkdownLivePresentation(
    private val selection: MarkdownTextRange,
    private val composition: MarkdownTextRange? = null,
    private val cache: MarkdownLiveCache = MarkdownLiveCache(),
) {
    fun filter(source: String): MarkdownLiveResult {
        val reveal = revealedLines(source)
        cache.whole(source, reveal)?.let { return it }
        val hidden = BooleanArray(source.length)
        val styles = mutableListOf<MarkdownLiveResult.Span>()
        var offset = 0
        var fence: String? = null
        for (line in source.split('\n')) {
            val end = offset + line.length
            val active = offset <= reveal.last && end >= reveal.first
            val marker = fencePattern.find(line)?.groupValues?.get(1)
            when {
                fence != null -> {
                    styles += MarkdownLiveResult.Span(offset, end, MarkdownLiveResult.Style(monospace = true))
                    if (line.trim().length >= fence.length && line.trim().all { it == fence.first() }) {
                        if (!active) hide(hidden, offset, end)
                        fence = null
                    }
                }
                marker != null -> {
                    fence = marker
                    styles += MarkdownLiveResult.Span(offset, end, MarkdownLiveResult.Style(monospace = true, accent = true))
                    if (!active) hide(hidden, offset, end)
                }
                line.startsWith("    ") || line.startsWith('\t') ->
                    styles.add(MarkdownLiveResult.Span(offset, end, MarkdownLiveResult.Style(monospace = true)))
                else -> cache.line(line, active) { decorateLine(line, active) }.applyAt(offset, hidden, styles)
            }
            offset = end + 1
        }
        return project(source, hidden, styles).also { cache.keep(source, reveal, it) }
    }

    private fun revealedLines(source: String): IntRange {
        val low = minOf(selection.min, composition?.min ?: selection.min).coerceIn(0, source.length)
        val high = maxOf(selection.max, composition?.max ?: selection.max).coerceIn(0, source.length)
        val start = if (low == 0) 0 else source.lastIndexOf('\n', low - 1) + 1
        val lastSelected = if (high > low) high - 1 else high
        val end = source.indexOf('\n', lastSelected).let { if (it < 0) source.length else it }
        return start..end
    }

    /** One line's decoration, counted from the line's start. */
    private fun decorateLine(line: String, active: Boolean): MarkdownLiveCache.LineDecoration {
        val hidden = BooleanArray(line.length)
        val styles = mutableListOf<MarkdownLiveResult.Span>()
        headingPattern.find(line)?.let { heading ->
            val level = heading.groupValues[1].length
            styles += MarkdownLiveResult.Span(0, line.length, MarkdownLiveResult.Style(bold = true, fontSize = 26 - level * 2))
            if (!active) hide(hidden, 0, heading.value.length)
        }
        val protected = BooleanArray(line.length)
        for (match in codePattern.findAll(line)) {
            if (escaped(line, match.range.first)) continue
            val delimiter = match.groupValues[1].length
            decorateToken(match.range, delimiter, delimiter, MarkdownLiveResult.Style(monospace = true), active, hidden, styles)
            for (i in match.range) protected[i] = true
        }
        for (match in linkPattern.findAll(line)) {
            if (escaped(line, match.range.first) || match.range.any { protected[it] } || line.getOrNull(match.range.first - 1) == '!') continue
            val label = match.groups[1] ?: continue
            styles += MarkdownLiveResult.Span(label.range.first, label.range.last + 1, MarkdownLiveResult.Style(accent = true, underline = true))
            if (!active) {
                hide(hidden, match.range.first, label.range.first)
                hide(hidden, label.range.last + 1, match.range.last + 1)
            }
            for (i in match.range) protected[i] = true
        }
        for ((pattern, delimiter, style) in emphasisPatterns) {
            for (match in pattern.findAll(line)) {
                if (escaped(line, match.range.first) || escaped(line, match.range.last - delimiter + 1) || match.range.any { protected[it] }) continue
                decorateToken(match.range, delimiter, delimiter, style, active, hidden, styles)
            }
        }
        if (!active) for (match in escapePattern.findAll(line)) {
            if (!protected[match.range.first] && !escaped(line, match.range.first)) hidden[match.range.first] = true
        }
        return MarkdownLiveCache.LineDecoration(hidden, styles)
    }

    private fun decorateToken(
        range: IntRange, prefix: Int, suffix: Int, style: MarkdownLiveResult.Style,
        active: Boolean, hidden: BooleanArray, styles: MutableList<MarkdownLiveResult.Span>,
    ) {
        styles += MarkdownLiveResult.Span(range.first + prefix, range.last + 1 - suffix, style)
        if (!active) {
            hide(hidden, range.first, range.first + prefix)
            hide(hidden, range.last + 1 - suffix, range.last + 1)
        }
    }

    private fun project(source: String, hidden: BooleanArray, styles: List<MarkdownLiveResult.Span>): MarkdownLiveResult {
        val forward = IntArray(source.length + 1)
        val visible = StringBuilder()
        for (i in source.indices) {
            forward[i] = visible.length
            if (!hidden[i]) visible.append(source[i])
        }
        forward[source.length] = visible.length
        val backward = IntArray(visible.length + 1)
        // Prefer the content side of hidden prefixes when the user clicks rendered text.
        for (i in forward.indices) backward[forward[i]] = i
        val ranges = mutableListOf<MarkdownTextRange>()
        var start = 0
        while (start < hidden.size) {
            if (!hidden[start]) { start++; continue }
            var end = start + 1
            while (end < hidden.size && hidden[end]) end++
            ranges += MarkdownTextRange(start,end)
            start = end
        }
        return MarkdownLiveResult(visible.toString(),ranges,styles,forward,backward)
    }

    private fun hide(hidden: BooleanArray, start: Int, end: Int) {
        for (i in start until end) hidden[i] = true
    }

    private fun escaped(text: String, offset: Int): Boolean {
        var start = offset
        while (start > 0 && text[start - 1] == '\\') start--
        return (offset - start) % 2 == 1
    }


    private companion object {
        val headingPattern = Regex("^ {0,3}(#{1,6})[ \\t]+")
        val fencePattern = Regex("^ {0,3}(`{3,}|~{3,})")
        val codePattern = Regex("(?<!`)(`+)([^`]+?)\\1(?!`)")
        val linkPattern = Regex("\\[([^\\[\\]]+)]\\([^()\\n]+\\)")
        val escapePattern = Regex("\\\\[\\\\`*_{}\\[\\]()#+.!~>-]")
        val emphasisPatterns = listOf(
            Triple(Regex("\\*\\*([^*]+)\\*\\*"), 2, MarkdownLiveResult.Style(bold = true)),
            Triple(Regex("(?<!\\w)__([^_]+)__(?!\\w)"), 2, MarkdownLiveResult.Style(bold = true)),
            Triple(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), 1, MarkdownLiveResult.Style(italic = true)),
            Triple(Regex("(?<![\\w_])_([^_]+)_(?![\\w_])"), 1, MarkdownLiveResult.Style(italic = true)),
            Triple(Regex("~~([^~]+)~~"), 2, MarkdownLiveResult.Style(strike = true)),
        )
    }
}
