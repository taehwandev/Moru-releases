package app.thdev.moru.core.ui.code

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import app.thdev.moru.core.designsystem.theme.MoruSyntaxColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeHighlighterTest {
    private val colors = MoruSyntaxColors.Light

    private fun colorsOf(text: String, language: CodeLanguage = CodeLanguage.Kotlin) =
        highlightCode(text, language, colors).spanStyles.associate { span -> text.substring(span.start, span.end) to span.item.color }

    @Test
    fun colorsEachKindOfToken() {
        val spans = colorsOf("@Composable fun Greet(name: String) = println(\"hi\" + 42) // done")

        assertEquals(colors.annotation, spans["@Composable"])
        assertEquals(colors.keyword, spans["fun"])
        assertEquals(colors.type, spans["Greet"])
        assertEquals(colors.type, spans["String"])
        assertEquals(colors.function, spans["println"])
        assertEquals(colors.string, spans["\"hi\""])
        assertEquals(colors.number, spans["42"])
        assertEquals(colors.comment, spans["// done"])
    }

    @Test
    fun aBlockCommentStaysColoredOnEveryLine() {
        val lines = highlightCode("/* one\ntwo */\nval x = 1", CodeLanguage.Kotlin, colors).codeLines()

        assertEquals(3, lines.size)
        assertEquals(colors.comment, lines[1].spanStyles.single().item.color)
        assertEquals("val x = 1", lines[2].text)
    }

    @Test
    fun anUnclosedQuoteEndsAtItsLine() {
        val lines = highlightCode("val s = \"open\nval t = 2", CodeLanguage.Kotlin, colors).codeLines()

        assertTrue(lines[1].spanStyles.any { span -> span.item.color == colors.keyword })
    }

    @Test
    fun unknownFilesStayPlain() {
        assertEquals(CodeLanguage.Plain, CodeLanguage.of("notes.txt"))
        assertTrue(highlightCode("fun x()", CodeLanguage.Plain, colors).spanStyles.isEmpty())
    }

    @Test
    fun aTrailingNewlineAddsNoEmptyLine() {
        assertEquals(listOf("a", "b"), highlightCode("a\nb\n", CodeLanguage.Plain, colors).codeLines().map { it.text })
        assertEquals(listOf(""), highlightCode("", CodeLanguage.Plain, colors).codeLines().map { it.text })
    }

    @Test
    fun utf16RangesAndCommentItalicsSurviveTheComposeAdapter() {
        val text = "😀 val face = \"😀\"\n/* open\nend */"
        val lines = highlightCode(text, CodeLanguage.Kotlin, colors).codeLines()
        assertEquals(listOf(3 to 6, 14 to 18), lines[0].spanStyles.map { it.start to it.end })
        assertEquals(listOf(colors.keyword, colors.string), lines[0].spanStyles.map { it.item.color })
        assertTrue(lines[0].spanStyles.all { it.item.fontStyle == null })
        assertEquals(FontStyle.Italic, lines[1].spanStyles.single().item.fontStyle)
        assertEquals(FontStyle.Italic, lines[2].spanStyles.single().item.fontStyle)
        assertEquals(colors.comment, lines[2].spanStyles.single().item.color)
    }

    @Test
    fun bothComposePalettesKeepTheirExactSourceColors() {
        fun values(palette: MoruSyntaxColors) = listOf(palette.keyword, palette.string, palette.number,
            palette.comment, palette.annotation, palette.type, palette.function)
        assertEquals(listOf(Color(0xFF8A2BB8), Color(0xFF1F7A3A), Color(0xFF1558B0), Color(0xFF7A7F87),
            Color(0xFF9A6200), Color(0xFF00737A), Color(0xFF2A55C9)), values(MoruSyntaxColors.Light))
        assertEquals(listOf(Color(0xFFCF8EF4), Color(0xFF9BD08A), Color(0xFF79B8FF), Color(0xFF8B929C),
            Color(0xFFE5C07B), Color(0xFF6FD3D8), Color(0xFF82AAFF)), values(MoruSyntaxColors.Dark))
    }

    @Test
    fun publicLanguageEnumKeepsItsEntriesAndDelegatedExtensionSelection() {
        assertEquals(listOf("Kotlin", "Java", "Swift", "Script", "Python", "Go", "Rust", "Shell", "Data", "Plain"),
            CodeLanguage.entries.map { it.name })
        assertEquals(CodeLanguage.Kotlin, CodeLanguage.of("build.GRADLE"))
        assertEquals(CodeLanguage.Swift, CodeLanguage.of("main.mm"))
        assertEquals(CodeLanguage.Script, CodeLanguage.of("view.tsx"))
        assertEquals(CodeLanguage.Plain, CodeLanguage.of("README.md"))
    }
}
