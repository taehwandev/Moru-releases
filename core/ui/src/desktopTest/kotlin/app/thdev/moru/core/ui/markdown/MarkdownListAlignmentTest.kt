package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Uses the rendered line metrics, rather than a body-font estimate, to check marker and gutter anchors. */
@OptIn(ExperimentalTestApi::class)
class MarkdownListAlignmentTest {
    @Test
    fun singleLineNormalBoldAndInlineCodeMarkersCenterOnTheTextLine() {
        draw(MarkdownBlock.ListItem("A normal single line."), "A normal single line.")
        draw(
            MarkdownBlock.ListItem("**Full test suite:** all 1,846 tests pass."),
            "Full test suite: all 1,846 tests pass.",
        )
        draw(
            MarkdownBlock.ListItem("Version `26.41.13` and bundle `app.thdev.moru`."),
            "Version 26.41.13 and bundle app.thdev.moru.",
        )
    }

    @Test
    fun wrappedAndExplicitMultilineMarkersStayAtTheFirstLine() {
        draw(
            MarkdownBlock.ListItem("**Packaging:** the DMG is signed, notarized and stapled by Apple, and Gatekeeper reports version `26.41.13` and bundle id `app.thdev.moru`."),
            "Packaging: the DMG is signed, notarized and stapled by Apple, and Gatekeeper reports version 26.41.13 and bundle id app.thdev.moru.",
            width = 360,
            multiline = true,
        )
        draw(
            MarkdownBlock.ListItem("First line\nSecond line\nThird line"),
            "First line\nSecond line\nThird line",
            multiline = true,
        )
    }

    @Test
    fun textScalingAndDifferentMarkersPreserveIndentAndFirstLineAlignment() {
        draw(
            MarkdownBlock.ListItem("**Full test suite:** all 1,846 tests pass.", depth = 2, number = "1"),
            "Full test suite: all 1,846 tests pass.",
            markerText = "1.",
            fontScale = 1.65f,
        )
        draw(
            MarkdownBlock.ListItem("A checked item\ncontinues on the next line.", checked = true),
            "A checked item\ncontinues on the next line.",
            markerText = "☑",
            fontScale = 1.65f,
            multiline = true,
        )
    }

    @Test
    fun nestedReadingViewPublishesTheEarliestTextLineAndUpdatesItWithoutAnExtraComposition() = runDesktopComposeUiTest(width = 700, height = 480) {
        var source by mutableStateOf("First paragraph.\n\nSecond paragraph.")
        var observedCenter = AlignmentLine.Unspecified
        setContent {
            MoruTheme {
                Layout(
                    modifier = Modifier.testTag("anchor-parent"),
                    content = { MarkdownReadingView(source, {}, padding = 17.dp) },
                ) { measurables, constraints ->
                    val content = measurables.single().measure(constraints)
                    observedCenter = content[MarkdownFirstLineCenter]
                    layout(content.width, content.height) { content.placeRelative(0, 0) }
                }
            }
        }
        fun checkFirstLine(display: String) {
            waitForIdle()
            val text = onNodeWithText(display)
            val expected = text.fetchSemanticsNode().boundsInRoot.top + text.textLayout().firstLineCenter()
            val actual = onNodeWithTag("anchor-parent").fetchSemanticsNode().boundsInRoot.top + observedCenter
            assertClose(expected, actual, "reading-view anchor")
        }
        checkFirstLine("First paragraph.")
        runOnIdle { source = "# A taller heading\n\nSecond paragraph." }
        checkFirstLine("A taller heading")
    }

    @Test
    fun inlineImagesKeepTheirOwnLayoutAndTheFirstTextSegmentPublishesItsOffset() = runDesktopComposeUiTest(width = 600, height = 480) {
        var observedCenter = AlignmentLine.Unspecified
        val loader = MarkdownImageLoader { _, _ -> MarkdownImageLoadResult.Unavailable }
        setContent {
            MoruTheme {
                Layout(
                    modifier = Modifier.testTag("anchor-parent"),
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(23.dp)) {
                            MarkdownInlineText("![](fixture.png)Text after the image.", {}, imageLoader = loader)
                            MarkdownInlineText("A later text segment.", {})
                        }
                    },
                ) { measurables, constraints ->
                    val content = measurables.single().measure(constraints)
                    observedCenter = content[MarkdownFirstLineCenter]
                    layout(content.width, content.height) { content.placeRelative(0, 0) }
                }
            }
        }
        waitForIdle()
        onNodeWithTag("markdown-image-unavailable").assertExists()
        val text = onNodeWithText("Text after the image.")
        val expected = text.fetchSemanticsNode().boundsInRoot.top + text.textLayout().firstLineCenter()
        val actual = onNodeWithTag("anchor-parent").fetchSemanticsNode().boundsInRoot.top + observedCenter
        assertClose(expected, actual, "text anchor after an image")
        assertTrue(observedCenter > text.textLayout().firstLineCenter(), "the image offset must propagate")
    }

    @Test
    fun emptyAndImageOnlyContentLeaveTheTextAnchorUnspecified() {
        for (source in listOf("", "![](fixture.png)")) {
            runDesktopComposeUiTest(width = 600, height = 480) {
                var observedCenter = AlignmentLine.Unspecified
                val loader = MarkdownImageLoader { _, _ -> MarkdownImageLoadResult.Unavailable }
                setContent {
                    MoruTheme {
                        Layout(
                            modifier = Modifier.testTag("anchor-parent"),
                            content = { MarkdownListItem(MarkdownBlock.ListItem(source), {}, null, loader) },
                        ) { measurables, constraints ->
                            val content = measurables.single().measure(constraints)
                            observedCenter = content[MarkdownFirstLineCenter]
                            layout(content.width, content.height) { content.placeRelative(0, 0) }
                        }
                    }
                }
                waitForIdle()
                assertEquals(AlignmentLine.Unspecified, observedCenter)
                val parent = onNodeWithTag("anchor-parent").fetchSemanticsNode().boundsInRoot
                val marker = onNodeWithText("•").fetchSemanticsNode().boundsInRoot
                assertClose(parent.top, marker.top, "image-only marker keeps top alignment")
            }
        }
    }

    private fun draw(
        item: MarkdownBlock.ListItem,
        display: String,
        markerText: String = "•",
        width: Int = 1_000,
        fontScale: Float = 1f,
        multiline: Boolean = false,
    ) = runDesktopComposeUiTest(width = width, height = 640) {
        var observedCenter = AlignmentLine.Unspecified
        var density = 1f
        var gapPx = 0f
        setContent {
            MoruTheme {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale)) {
                    density = LocalDensity.current.density
                    gapPx = with(LocalDensity.current) { MoruTheme.spacing.small.toPx() }
                    Layout(
                        modifier = Modifier.testTag("anchor-parent"),
                        content = {
                            Column(Modifier.padding(top = 17.dp)) {
                                MarkdownListItem(item, {}, null, null)
                            }
                        },
                    ) { measurables, constraints ->
                        val content = measurables.single().measure(constraints)
                        observedCenter = content[MarkdownFirstLineCenter]
                        layout(content.width, content.height) { content.placeRelative(0, 0) }
                    }
                }
            }
        }
        waitForIdle()
        val text = onNodeWithText(display)
        val layout = text.textLayout()
        assertEquals(multiline, layout.lineCount > 1, "fixture line count")
        val textBounds = text.fetchSemanticsNode().boundsInRoot
        val markerBounds = onNodeWithText(markerText).fetchSemanticsNode().boundsInRoot
        val parentBounds = onNodeWithTag("anchor-parent").fetchSemanticsNode().boundsInRoot
        val expectedCenter = textBounds.top + layout.firstLineCenter()
        assertClose(expectedCenter, markerBounds.center.y, "list marker center")
        assertClose(expectedCenter, parentBounds.top + observedCenter, "propagated first-line anchor")
        assertClose((if (item.number == null) 20f else 36f) * density, markerBounds.width, "marker width")
        assertClose(18f * item.depth.coerceAtMost(12) * density, markerBounds.left - parentBounds.left, "list indentation")
        assertClose(gapPx, textBounds.left - markerBounds.right, "marker gap")
        if (multiline) {
            assertTrue(markerBounds.center.y < textBounds.center.y, "a multiline marker must not center on the whole block")
        }
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }

    private fun TextLayoutResult.firstLineCenter(): Float = (getLineTop(0) + getLineBottom(0)) / 2f

    private fun assertClose(expected: Float, actual: Float, label: String) {
        assertTrue(abs(expected - actual) <= 1f, "$label: expected $expected, actual $actual")
    }
}
