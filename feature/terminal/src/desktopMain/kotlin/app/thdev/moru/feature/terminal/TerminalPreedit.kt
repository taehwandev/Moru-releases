package app.thdev.moru.feature.terminal

import java.awt.Point
import java.awt.event.InputMethodEvent
import java.text.CharacterIterator

/** Native IME marked text only; committed text and the VT cursor remain owned by JediTerm. */
internal class TerminalPreedit {
    var text: String = ""
        private set
    var caret: Int = 0
        private set
    private var origin = Point()
    private var nextOrigin: Point? = null
    private var cursorAtCommit: Point? = null
    val active: Boolean get() = text.isNotEmpty()

    fun update(event: InputMethodEvent, columns: Int, cursor: () -> Point) {
        if (event.id == InputMethodEvent.CARET_POSITION_CHANGED) {
            caret = (event.caret?.insertionIndex ?: caret).coerceIn(0, text.length)
            return
        }
        val incoming = event.text?.let { iterator ->
            buildString {
                var character = iterator.first()
                while (character != CharacterIterator.DONE) {
                    append(character)
                    character = iterator.next()
                }
            }
        }.orEmpty()
        val committed = incoming.take(event.committedCharacterCount).printable()
        val remaining = incoming.drop(event.committedCharacterCount).printable()
        val observed = cursor()
        val start = if (active) origin else nextOrigin?.takeIf { observed == cursorAtCommit } ?: observed
        origin = TerminalPreeditLayout(committed, start, columns).end
        text = remaining
        caret = (event.caret?.insertionIndex ?: remaining.length).coerceIn(0, remaining.length)
        // Consecutive syllables can arrive before the asynchronous PTY echo updates the screen cursor.
        nextOrigin = if (committed.isNotEmpty()) Point(origin) else null
        cursorAtCommit = if (committed.isNotEmpty()) observed else null
    }

    fun layout(columns: Int): TerminalPreeditLayout? =
        if (active) TerminalPreeditLayout(text, origin, columns) else null

    /** Drops marked text whose input method session ended elsewhere; the commit for it will never arrive. */
    fun abandon() {
        text = ""
        caret = 0
        forgetCommittedOrigin()
    }

    fun forgetCommittedOrigin() {
        nextOrigin = null
        cursorAtCommit = null
    }

    private fun String.printable(): String = filter { it >= ' ' && it != '\u007f' }
}
