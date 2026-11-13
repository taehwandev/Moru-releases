package app.thdev.moru.feature.terminal.search

import com.jediterm.terminal.StyledTextConsumer
import com.jediterm.terminal.SubstringFinder
import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.model.CharBuffer
import com.jediterm.terminal.model.SubCharBuffer
import com.jediterm.terminal.model.TerminalTextBuffer

/** Uses the same cell buffers as JediTerm's painter, so ANSI styles and wide cells retain their highlights. */
internal object TerminalBufferSearch {
    /** The caller holds the buffer lock while matching and installing the result. */
    fun find(buffer: TerminalTextBuffer, query: String): SubstringFinder.FindResult? {
        if (query.isEmpty()) return null
        val finder = SubstringFinder(query, true)
        buffer.processHistoryAndScreenLines(-buffer.historyLinesCount, -1, object : StyledTextConsumer {
            override fun consume(x: Int, y: Int, style: TextStyle, characters: CharBuffer, startRow: Int) {
                val parent = (characters as? SubCharBuffer)?.parent ?: characters
                val offset = (characters as? SubCharBuffer)?.offset ?: 0
                repeat(characters.length) { index -> finder.nextChar(x, y - startRow, parent, offset + index) }
            }

            override fun consumeNul(x: Int, y: Int, nulIndex: Int, style: TextStyle, characters: CharBuffer, startRow: Int) = Unit
            override fun consumeQueue(x: Int, y: Int, nulIndex: Int, startRow: Int) = Unit
        })
        return finder.result
    }
}
