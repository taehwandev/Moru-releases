package app.thdev.moru.core.domain.markdown


/** Source edits shared by toolbar and keyboard; all positions are original Markdown offsets. */
object MarkdownEditingActions {
    fun wrap(value: MarkdownEditValue, marker: String, closing: String = marker): MarkdownEditValue {
        val start = value.selection.min
        val end = value.selection.max
        val selected = value.text.substring(start, end)
        if (selected.startsWith(marker) && selected.endsWith(closing) && selected.length >= marker.length + closing.length) {
            val content = selected.substring(marker.length, selected.length - closing.length)
            return replace(value, start, end, content, MarkdownTextRange(start, start + content.length))
        }
        if (start >= marker.length && value.text.substring(start - marker.length, start) == marker &&
            value.text.substring(end).startsWith(closing)) {
            return replace(value, start - marker.length, end + closing.length, selected, MarkdownTextRange(start - marker.length, end - marker.length))
        }
        return replace(value, start, end, marker + selected + closing, MarkdownTextRange(start + marker.length, end + marker.length))
    }

    fun link(value: MarkdownEditValue): MarkdownEditValue {
        val start = value.selection.min
        val label = value.text.substring(start, value.selection.max).ifEmpty { "link" }
        val targetStart = start + label.length + 3
        return replace(value, start, value.selection.max, "[$label](url)", MarkdownTextRange(targetStart, targetStart + 3))
    }

    fun prefix(value: MarkdownEditValue, prefix: String): MarkdownEditValue {
        val start = if (value.selection.min == 0) 0 else value.text.lastIndexOf('\n', value.selection.min - 1) + 1
        val last = if (value.selection.collapsed) value.selection.max else value.selection.max - 1
        val end = value.text.indexOf('\n', last).let { if (it < 0) value.text.length else it }
        val lines = value.text.substring(start, end).split('\n')
        val remove = lines.all { it.startsWith(prefix) }
        val replacement = lines.joinToString("\n") { if (remove) it.removePrefix(prefix) else prefix + it }
        val selection = if (value.selection.collapsed) {
            MarkdownTextRange((value.selection.start + if (remove) -prefix.length else prefix.length).coerceIn(start, start + replacement.length))
        } else MarkdownTextRange(start, start + replacement.length)
        return replace(value, start, end, replacement, selection)
    }

    private fun replace(value: MarkdownEditValue, start: Int, end: Int, text: String, selection: MarkdownTextRange): MarkdownEditValue =
        MarkdownEditValue(value.text.replaceRange(start, end, text), selection)
}
