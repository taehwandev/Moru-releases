package app.thdev.moru.core.domain.markdown


/** Bounded source history; selection moves and unfinished IME composition are not separate edits. */
class MarkdownEditHistory(initial: MarkdownEditValue) {
    var value: MarkdownEditValue = initial
        private set
    private val past = ArrayDeque<MarkdownEditValue>()
    private val future = ArrayDeque<MarkdownEditValue>()
    private var composingFrom: MarkdownEditValue? = null

    fun accept(next: MarkdownEditValue): MarkdownEditValue {
        if (next.composition != null) {
            if (composingFrom == null) composingFrom = value.copy(composition = null)
        } else {
            val previous = composingFrom ?: value
            composingFrom = null
            if (next.text != previous.text) {
                past.addLast(previous.copy(composition = null))
                future.clear()
                trim(past)
            }
        }
        value = next
        return value
    }

    fun undo(): MarkdownEditValue {
        if (composingFrom != null) accept(value.copy(composition = null))
        if (past.isNotEmpty()) {
            future.addLast(value.copy(composition = null))
            value = past.removeLast()
            trim(future)
        }
        return value
    }

    fun redo(): MarkdownEditValue {
        if (future.isNotEmpty()) {
            past.addLast(value.copy(composition = null))
            value = future.removeLast()
            trim(past)
        }
        return value
    }

    private fun trim(entries: ArrayDeque<MarkdownEditValue>) {
        var size = entries.sumOf { it.text.length.toLong() }
        while (entries.size > 100 || size > 1_000_000) size -= entries.removeFirst().text.length
    }
}
