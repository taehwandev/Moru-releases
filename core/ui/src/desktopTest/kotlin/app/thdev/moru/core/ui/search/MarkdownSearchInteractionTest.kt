package app.thdev.moru.core.ui.search

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.markdown.LocalMarkdownLinkHints
import app.thdev.moru.core.ui.markdown.MarkdownLinkHints
import app.thdev.moru.core.ui.markdown.MarkdownReadingView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MarkdownSearchInteractionTest {
    @Test
    fun aHighlightedLinkKeepsItsTargetAndCharactersWhenAnInlineIconPrecedesIt() = runDesktopComposeUiTest(width = 700, height = 350) {
        val source = "Open [**needle** guide](notes.md) then `needle`."
        val active = markdownSearchLeaves(source, "reply").flatMap { it.matches("needle") }.first()
        val opened = mutableListOf<String>()
        setContent {
            MoruTheme {
                CompositionLocalProvider(
                    LocalMarkdownLinkHints provides MarkdownLinkHints({ false }, {}),
                    LocalTextSearchScope provides TextSearchScope("needle", active, "reply"),
                ) {
                    MarkdownReadingView(source, opened::add)
                }
            }
        }
        val node = onNodeWithTag("text-search-active")
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val text = layouts.single().layoutInput.text
        assertEquals("Open needle guide then needle.", text.text.replace("\u2060", ""))
        val renderedStart = text.text.indexOf("needle")
        assertEquals(active.start + 1, renderedStart, "The injected link icon must not shift the logical match")
        assertTrue(text.spanStyles.any { it.start == renderedStart && it.end == renderedStart + 6 && it.item.background.alpha > 0.9f })
        assertTrue(text.getLinkAnnotations(renderedStart, renderedStart + 6).isNotEmpty())
        node.performTouchInput { click(layouts.single().getBoundingBox(renderedStart).center) }
        runOnIdle { assertEquals(listOf("notes.md"), opened) }
    }
}
