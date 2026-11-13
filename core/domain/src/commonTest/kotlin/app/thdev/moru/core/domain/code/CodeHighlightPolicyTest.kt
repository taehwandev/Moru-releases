package app.thdev.moru.core.domain.code

import app.thdev.moru.core.model.code.CodeSyntaxToken
import app.thdev.moru.core.model.code.CodeSyntaxToken.Kind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeHighlightPolicyTest {
    @Test fun retainsEveryTokenKindAndOnlyCommentsAreItalic() {
        val text = "@Composable fun Greet(name: String) = println(\"hi\" + 42) // done"
        val tokens = CodeHighlightPolicy.tokens(text, CodeLanguage.Kotlin)
        assertEquals(listOf("@Composable", "fun", "Greet", "String", "println", "\"hi\"", "42", "// done"),
            tokens.map { text.substring(it.start, it.end) })
        assertEquals(listOf(Kind.Annotation, Kind.Keyword, Kind.Type, Kind.Type, Kind.Function, Kind.String, Kind.Number, Kind.Comment),
            tokens.map { it.kind })
        assertEquals(listOf(false, false, false, false, false, false, false, true), tokens.map { it.italic })
    }

    @Test fun extensionCategoriesKeepTheExistingAliasesAndCaseHandling() {
        val extensions = mapOf(
            CodeLanguage.Kotlin to listOf("kt", "kts", "gradle"),
            CodeLanguage.Java to listOf("java", "c", "h", "cpp", "hpp", "cs", "dart", "scala"),
            CodeLanguage.Swift to listOf("swift", "m", "mm"),
            CodeLanguage.Script to listOf("js", "jsx", "ts", "tsx", "mjs", "cjs"),
            CodeLanguage.Python to listOf("py"), CodeLanguage.Go to listOf("go"), CodeLanguage.Rust to listOf("rs"),
            CodeLanguage.Shell to listOf("sh", "zsh", "bash"),
            CodeLanguage.Data to listOf("json", "yaml", "yml", "toml", "properties"),
        )
        for ((language, names) in extensions) for (extension in names) {
            assertEquals(language, CodeLanguage.of("folder/source.$extension"))
            assertEquals(language, CodeLanguage.of("folder/source.${extension.uppercase()}"))
        }
        for (path in listOf("README", "notes.txt", "page.html", "style.css", "README.md", "Dockerfile")) {
            assertEquals(CodeLanguage.Plain, CodeLanguage.of(path))
        }
    }

    @Test fun offsetsUseUtf16BeforeAndInsideAString() {
        val text = "😀 val face = \"😀\""
        assertEquals(listOf(CodeSyntaxToken(3, 6, Kind.Keyword), CodeSyntaxToken(14, 18, Kind.String)),
            CodeHighlightPolicy.tokens(text, CodeLanguage.Kotlin))
    }

    @Test fun multilineCommentsAreClippedToRowsAfterWholeFileClassification() {
        val text = "😀 /* one\nmiddle\nend */ val x = 1\n"
        val lines = CodeHighlightPolicy.lineTokens(text, CodeLanguage.Kotlin)
        assertEquals(3, lines.size)
        assertEquals(listOf(CodeSyntaxToken(3, 9, Kind.Comment)), lines[0])
        assertEquals(listOf(CodeSyntaxToken(0, 6, Kind.Comment)), lines[1])
        assertEquals(listOf(CodeSyntaxToken(0, 6, Kind.Comment), CodeSyntaxToken(7, 10, Kind.Keyword),
            CodeSyntaxToken(15, 16, Kind.Number)), lines[2])
    }

    @Test fun tripleStringsAndScriptBackticksKeepCommentLikeLinesAsStrings() {
        val samples = listOf(
            CodeLanguage.Kotlin to "val text = \"\"\"one\n// hidden val\nend\"\"\"\nfun after()",
            CodeLanguage.Python to "value = '''one\n# hidden def\nend'''\ndef after(): pass",
            CodeLanguage.Script to "const text = `one\n// hidden function\nend`;\nfunction after() {}",
        )
        for ((language, text) in samples) {
            val lines = CodeHighlightPolicy.lineTokens(text, language)
            val hidden = text.substringAfter('\n').substringBefore('\n')
            assertEquals(listOf(CodeSyntaxToken(0, hidden.length, Kind.String)), lines[1])
            assertTrue(lines.last().any { it.kind == Kind.Keyword })
            assertTrue(CodeHighlightPolicy.tokens(text, language).any { it.kind == Kind.String && '\n' in text.substring(it.start, it.end) })
        }
    }

    @Test fun escapesAndUnterminatedQuotesPreserveTheReadersNewlineRules() {
        val text = "val a = \"escaped \\\" value\";\nval b = \"open\nval c = 2"
        val strings = CodeHighlightPolicy.tokens(text, CodeLanguage.Kotlin).filter { it.kind == Kind.String }
        assertEquals(listOf("\"escaped \\\" value\"", "\"open"), strings.map { text.substring(it.start, it.end) })
        assertTrue(CodeHighlightPolicy.lineTokens(text, CodeLanguage.Kotlin)[2].any { it.kind == Kind.Keyword })
        // An escaped LF is skipped by the original scanner, while an ordinary LF ends a quoted string.
        val escapedLine = "\"one\\\ntwo\""
        assertEquals(listOf(CodeSyntaxToken(0, escapedLine.length, Kind.String)), CodeHighlightPolicy.tokens(escapedLine, CodeLanguage.Kotlin))
        val trailingEscape = "\"tail\\"
        assertEquals(listOf(CodeSyntaxToken(0, trailingEscape.length, Kind.String)), CodeHighlightPolicy.tokens(trailingEscape, CodeLanguage.Kotlin))
    }

    @Test fun crlfAndEmptyLinesRetainLocalOffsetsWithoutPhantomFinalRows() {
        val lines = CodeHighlightPolicy.lineTokens("/* a\r\nb */\r\nval x=1\r\n", CodeLanguage.Kotlin)
        assertEquals(3, lines.size)
        assertEquals(listOf(CodeSyntaxToken(0, 5, Kind.Comment)), lines[0])
        assertEquals(listOf(CodeSyntaxToken(0, 4, Kind.Comment)), lines[1])
        assertEquals(listOf(CodeSyntaxToken(0, 3, Kind.Keyword), CodeSyntaxToken(6, 7, Kind.Number)), lines[2])
        assertEquals(listOf(emptyList<CodeSyntaxToken>(), emptyList()), CodeHighlightPolicy.lineTokens("a\nb\n", CodeLanguage.Kotlin))
        assertEquals(listOf(emptyList<CodeSyntaxToken>()), CodeHighlightPolicy.lineTokens("", CodeLanguage.Kotlin))
    }

    @Test fun unclosedMultilineTokensReachTheEndWithoutStylingEmptyRows() {
        val comment = CodeHighlightPolicy.lineTokens("/*\n\nlast", CodeLanguage.Kotlin)
        assertEquals(listOf(CodeSyntaxToken(0, 2, Kind.Comment)), comment[0])
        assertTrue(comment[1].isEmpty())
        assertEquals(listOf(CodeSyntaxToken(0, 4, Kind.Comment)), comment[2])
        val triple = "\"\"\"open\nlast"
        assertEquals(listOf(CodeSyntaxToken(0, triple.length, Kind.String)), CodeHighlightPolicy.tokens(triple, CodeLanguage.Kotlin))
        val backtick = "`open\nlast"
        assertEquals(listOf(CodeSyntaxToken(0, backtick.length, Kind.String)), CodeHighlightPolicy.tokens(backtick, CodeLanguage.Script))
    }

    @Test fun retainsTypePrecedenceAsciiSpaceCallLookaheadAndBroadNumbers() {
        val text = "fun Call() call () spaced\t() value1 0xFF_FF.0 /* outer /* inner */ val after = 2 */"
        val tokens = CodeHighlightPolicy.tokens(text, CodeLanguage.Kotlin)
        val kinds = tokens.associate { text.substring(it.start, it.end) to it.kind }
        assertEquals(Kind.Keyword, kinds["fun"])
        assertEquals(Kind.Type, kinds["Call"])
        assertEquals(Kind.Function, kinds["call"])
        assertFalse("spaced" in kinds)
        assertFalse("value1" in kinds)
        assertEquals(Kind.Number, kinds["0xFF_FF.0"])
        assertEquals(Kind.Keyword, kinds["val"]) // Block comments still stop at the first closing delimiter.
        assertTrue(CodeHighlightPolicy.tokens("fun Type(42) // comment", CodeLanguage.Plain).isEmpty())
    }
}
