package app.thdev.moru.core.ui.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MarkdownStreamParserTest {
    private val answer = """
        # Plan

        First a paragraph that runs
        over two lines.

        - one
          - [x] nested
        1. numbered

        > quoted
        > more

        | name | size |
        | --- | ---: |
        | a | 1 |

        ```kotlin
        val a = 1

        val b = 2
        ```

        ~~~~
        ```
        still code

        ~~~~

        ---
        Last words.
    """.trimIndent()

    @Test
    fun everyStepOfAGrowingAnswerReadsAsTheWholeTextWould() {
        val stream = MarkdownStreamParser()
        for (end in 0..answer.length) {
            val text = answer.substring(0, end)
            assertEquals(parseMarkdownBlocks(text), stream.parse(text), "after ${end} characters")
        }
    }

    @Test
    fun finishedBlocksAreKeptWhileTheEndGrows() {
        val stream = MarkdownStreamParser()
        val first = stream.parse("# Title\n\nSome text")
        val second = stream.parse("# Title\n\nSome text that grows")
        assertSame(first.first(), second.first())
    }

    @Test
    fun textThatNoLongerStartsTheSameIsReadAgain() {
        val stream = MarkdownStreamParser()
        stream.parse("# Old\n\nbody")
        assertEquals(parseMarkdownBlocks("New\n\nbody"), stream.parse("New\n\nbody"))
    }

    @Test
    fun aBlankLineInsideAnOpenFenceIsNotABoundary() {
        assertEquals(0, settledBoundary("```\ncode\n\nmore\n", 0))
        assertEquals(11, settledBoundary("```\nx\n```\n\nafter", 0))
    }
}
