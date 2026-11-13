package app.thdev.moru.core.ui.code

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeLinesTest {
    @Test fun domainOffsetsKeepAnnotationsAndDropOnlyTheFinalPhantomLine() {
        val style = SpanStyle(color = Color.Red)
        val input = AnnotatedString("a\nb\n", listOf(AnnotatedString.Range(style, 2, 3)))
        val lines = input.codeLines()
        assertEquals(listOf("a", "b"), lines.map { it.text })
        assertEquals(listOf(AnnotatedString.Range(style, 0, 1)), lines[1].spanStyles)
        assertEquals(listOf(""), AnnotatedString("").codeLines().map { it.text })
    }
}
