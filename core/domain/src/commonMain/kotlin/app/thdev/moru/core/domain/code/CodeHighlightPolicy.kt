package app.thdev.moru.core.domain.code

import app.thdev.moru.core.domain.git.ChangedCodePolicy
import app.thdev.moru.core.model.code.CodeSyntaxToken
import app.thdev.moru.core.model.code.CodeSyntaxToken.Kind

/** The existing whole-file reader scanner. It colors heuristically and never edits or reads files. */
object CodeHighlightPolicy {
    fun tokens(text: String, language: CodeLanguage): List<CodeSyntaxToken> {
        if (language == CodeLanguage.Plain) return emptyList()
        val tokens = mutableListOf<CodeSyntaxToken>()
        fun mark(start: Int, end: Int, kind: Kind) {
            tokens += CodeSyntaxToken(start, end.coerceAtMost(text.length), kind)
        }
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val lineComment = language.lineComment
            when {
                lineComment != null && text.startsWith(lineComment, i) -> {
                    val end = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                    mark(i, end, Kind.Comment)
                    i = end
                }
                language.blockComments && text.startsWith("/*", i) -> {
                    val end = text.indexOf("*/", i + 2).let { if (it < 0) text.length else it + 2 }
                    mark(i, end, Kind.Comment)
                    i = end
                }
                language.tripleStrings && (text.startsWith("\"\"\"", i) || text.startsWith("'''", i)) -> {
                    val quote = text.substring(i, i + 3)
                    val end = text.indexOf(quote, i + 3).let { if (it < 0) text.length else it + 3 }
                    mark(i, end, Kind.String)
                    i = end
                }
                c == '"' || c == '\'' || (c == '`' && language == CodeLanguage.Script) -> {
                    val end = stringEnd(text, i, c)
                    mark(i, end, Kind.String)
                    i = end
                }
                c == '@' && i + 1 < text.length && text[i + 1].isLetter() -> {
                    val end = wordEnd(text, i + 1)
                    mark(i, end, Kind.Annotation)
                    i = end
                }
                c.isDigit() && (i == 0 || !text[i - 1].isWordPart()) -> {
                    var end = i + 1
                    while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_' || text[end] == '.')) end++
                    mark(i, end, Kind.Number)
                    i = end
                }
                c.isLetter() || c == '_' -> {
                    val end = wordEnd(text, i)
                    val word = text.substring(i, end)
                    val next = (end until text.length).firstOrNull { index -> text[index] != ' ' }?.let(text::get)
                    when {
                        word in language.keywords -> mark(i, end, Kind.Keyword)
                        word[0].isUpperCase() -> mark(i, end, Kind.Type)
                        next == '(' -> mark(i, end, Kind.Function)
                    }
                    i = end
                }
                else -> i++
            }
        }
        return tokens
    }

    /** Classify before clipping, so hidden lines can begin comments/strings visible in later rows. */
    fun lineTokens(
        text: String,
        language: CodeLanguage,
        slices: List<ChangedCodePolicy.LineSlice> = ChangedCodePolicy.lineSlices(text),
    ): List<List<CodeSyntaxToken>> {
        val tokens = tokens(text, language)
        var first = 0
        return slices.map { slice ->
            while (first < tokens.size && tokens[first].end <= slice.start) first++
            var current = first
            buildList {
                while (current < tokens.size && tokens[current].start < slice.end) {
                    val token = tokens[current++]
                    val start = maxOf(token.start, slice.start)
                    val end = minOf(token.end, slice.end)
                    if (start < end) add(token.copy(start = start - slice.start, end = end - slice.start))
                }
            }
        }
    }

    // Preserve the reader's escape and newline rules, including multiline Script backticks.
    private fun stringEnd(text: String, start: Int, quote: Char): Int {
        var i = start + 1
        while (i < text.length) {
            when (text[i]) {
                '\\' -> i += 2
                quote -> return i + 1
                '\n' -> if (quote != '`') return i else i++
                else -> i++
            }
        }
        return text.length
    }

    private fun wordEnd(text: String, start: Int): Int {
        var end = start
        while (end < text.length && text[end].isWordPart()) end++
        return end
    }
    private fun Char.isWordPart() = isLetterOrDigit() || this == '_'
}
