package app.thdev.moru.core.domain.markdown


/** Conservative list edits in Markdown source offsets; null leaves the key to the text field. */
object MarkdownListEditing {
    private val itemPattern = Regex("""^([ \t]*)([-+*]|[0-9]{1,9}[.)])([ \t]+)(.*)$""")
    private val taskPattern = Regex("""^\[([ xX])]([ \t]+|$)(.*)$""")
    private val fencePattern = Regex("""^[ \t]*(`{3,}|~{3,})(.*)$""")
    private val thematicBreak = Regex("""(?:-[ \t]*){3,}|(?:\*[ \t]*){3,}|(?:_[ \t]*){3,}""")

    fun enter(value: MarkdownEditValue): MarkdownEditValue? {
        if (value.composition != null || !value.selection.collapsed) return null
        val caret = value.selection.start
        val lines = lines(value.text)
        val line = lines.lastOrNull { it.start <= caret } ?: return null
        if (line.fenced || caret > line.end) return null
        val item = item(value.text.substring(line.start, line.end)) ?: return null
        if (caret < line.start + item.prefix.length) return null
        if (item.body.isBlank()) {
            val replacement = if (item.indent.isEmpty()) "" else {
                val count = if (item.indent.endsWith('\t')) 1 else item.indent.takeLastWhile { it == ' ' }.length.coerceAtMost(2)
                item.indent.dropLast(count) + item.prefix.removePrefix(item.indent)
            }
            return edit(value, listOf(Edit(line.start, line.end, replacement)), MarkdownTextRange(line.start + replacement.length))
        }
        val nextMarker = if (item.marker.first().isDigit()) {
            val digits = item.marker.dropLast(1)
            val next = (digits.toLong() + 1).toString().padStart(digits.length, '0')
            if (next.length > 9) return null
            next + item.marker.last()
        } else item.marker
        val prefix = item.indent + nextMarker + item.spacing + item.taskPrefix
        val newline = newline(value.text, lines, line)
        return edit(value, listOf(Edit(caret, caret, newline + prefix)), MarkdownTextRange(caret + newline.length + prefix.length))
    }

    fun indent(value: MarkdownEditValue, outdent: Boolean): MarkdownEditValue? {
        if (value.composition != null) return null
        val first = value.selection.min
        val last = if (value.selection.collapsed) first else value.selection.max - 1
        val lines = lines(value.text)
        val selected = lines.filter { it.start <= last && (it.end >= first || it.start == first) }
        if (selected.isEmpty() || selected.any { it.fenced }) return null
        val items = selected.map { item(value.text.substring(it.start, it.end)) ?: return null }
        // Every selected item moves equally, so nested children keep their relative depth.
        val common = items.map { it.indent }.reduce { prefix, next -> prefix.commonPrefixWith(next) }
        val edits = if (outdent) {
            if (common.isEmpty()) return null
            val count = if (common.startsWith('\t')) 1 else common.takeWhile { it == ' ' }.length.coerceAtMost(2)
            selected.map { Edit(it.start, it.start + count, "") }
        } else {
            val prefix = if (items.first().indent.startsWith('\t')) "\t" else "  "
            selected.map { Edit(it.start, it.start, prefix) }
        }
        return edit(value, edits)
    }

    private fun item(source: String): Item? {
        if (thematicBreak.matches(source.trim())) return null
        val match = itemPattern.matchEntire(source) ?: return null
        val (indent, marker, spacing, content) = match.destructured
        val task = taskPattern.matchEntire(content)
        val taskPrefix = if (task == null) "" else "[ ]" + task.groupValues[2].ifEmpty { " " }
        val body = task?.groupValues?.get(3) ?: content
        // The existing checkbox's source width determines where the editable body begins.
        val sourceTaskLength = if (task == null) 0 else content.length - body.length
        return Item(indent, marker, spacing, taskPrefix, body, source.take(indent.length + marker.length + spacing.length + sourceTaskLength))
    }

    private fun lines(text: String): List<Line> {
        val result = mutableListOf<Line>()
        var start = 0
        var fenceChar: Char? = null
        var fenceLength = 0
        while (true) {
            val newline = text.indexOf('\n', start)
            val rawEnd = if (newline < 0) text.length else newline
            val end = if (rawEnd > start && text[rawEnd - 1] == '\r') rawEnd - 1 else rawEnd
            val source = text.substring(start, end)
            // Fences may be the first content of a list item, including nested list items.
            val listContent = itemPattern.matchEntire(source)?.groupValues?.get(4)
            val content = listContent?.let { taskPattern.matchEntire(it)?.groupValues?.get(3) ?: it } ?: source
            // Inside code, a literal list marker before backticks cannot close the fence.
            val fence = fencePattern.matchEntire(if (fenceChar != null) source else content)
            val marker = fence?.groupValues?.get(1)
            val suffix = fence?.groupValues?.get(2)
            var blocked = fenceChar != null
            if (fenceChar != null) {
                if (marker != null && marker.first() == fenceChar && marker.length >= fenceLength && suffix.orEmpty().isBlank()) {
                    fenceChar = null
                }
            } else if (marker != null && (marker.first() != '`' || !suffix.orEmpty().contains('`'))) {
                blocked = true
                fenceChar = marker.first()
                fenceLength = marker.length
            }
            result += Line(start, end, blocked)
            if (newline < 0) return result
            start = newline + 1
        }
    }

    private fun newline(text: String, lines: List<Line>, line: Line): String {
        // Prefer this line's ending, then a preceding line's style for the final line.
        val reference = if (line.end < text.length) line else lines.lastOrNull { it.end < text.length }
        return if (reference != null && text.getOrNull(reference.end) == '\r') "\r\n" else "\n"
    }

    private fun edit(value: MarkdownEditValue, edits: List<Edit>, selection: MarkdownTextRange? = null): MarkdownEditValue {
        var text = value.text
        for (edit in edits.asReversed()) text = text.replaceRange(edit.start, edit.end, edit.text)
        fun offset(original: Int): Int {
            var delta = 0
            for (edit in edits) {
                if (original < edit.start) break
                if (original < edit.end) return edit.start + delta + edit.text.length
                delta += edit.text.length - (edit.end - edit.start)
            }
            return original + delta
        }
        return MarkdownEditValue(text, selection ?: MarkdownTextRange(offset(value.selection.start), offset(value.selection.end)))
    }

    private data class Item(
        val indent: String,
        val marker: String,
        val spacing: String,
        val taskPrefix: String,
        val body: String,
        val prefix: String,
    )

    private data class Line(val start: Int, val end: Int, val fenced: Boolean)
    private data class Edit(val start: Int, val end: Int, val text: String)
}
