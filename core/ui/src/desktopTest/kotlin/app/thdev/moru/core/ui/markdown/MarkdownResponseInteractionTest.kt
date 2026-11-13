package app.thdev.moru.core.ui.markdown

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.component.MoruButton
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MarkdownResponseInteractionTest {
    @Test
    fun copyCodePreservesTheExactTextAndShowsFeedback() = runDesktopComposeUiTest {
        val clipboard = FakeClipboard()
        val code = "  val text = \"https://example.org\"\n\tprintln(text)"
        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                MoruTheme { MarkdownReadingView("```kotlin\n$code\n```", {}) }
            }
        }
        onNodeWithText("Copy code").performClick()
        onNodeWithText("Copied").assertIsDisplayed()
        runOnIdle { assertEquals(code, clipboard.held?.text) }
    }

    @Test
    fun fifthPositionalCodeActionReplacesDefaultCopyAndKeepsImages() = runDesktopComposeUiTest {
        val code = "  println(\"Preview\")"
        val submitted = mutableListOf<String>()
        val opened = mutableListOf<String>()
        val loader = MarkdownImageLoader { _, _ ->
            MarkdownImageLoadResult.Available(ImageBitmap(8, 4), "/project/preview.png")
        }
        setContent {
            MoruTheme {
                MarkdownReadingView(
                    "```kotlin\n$code\n```\n\n![Preview](preview.png)",
                    opened::add,
                    Modifier,
                    null,
                    { text -> MoruButton("Use code", { submitted += text }) },
                    imageBasePath = "/project",
                    imageLoader = loader,
                )
            }
        }
        onNodeWithTag("markdown-copy-code").assertDoesNotExist()
        onNodeWithText("Use code").performClick()
        waitForIdle()
        onNodeWithContentDescription("Preview").performClick()
        runOnIdle {
            assertEquals(listOf(code), submitted)
            assertEquals(listOf("/project/preview.png"), opened)
        }
    }

    @Test
    fun bareWebAndLocalLinksOpenAndUnsafeSchemesStayInert() = runDesktopComposeUiTest {
        val web = mutableListOf<String>()
        val local = mutableListOf<String>()
        setContent {
            CompositionLocalProvider(LocalUriHandler provides object : UriHandler {
                override fun openUri(uri: String) { web += uri }
            }) {
                MoruTheme { MarkdownReadingView("https://example.org\n\n[Guide](guide.md)\n\n[File URI](file:///project/title.kt)\n\n[Source line](title.kt:12)\n\n[Unsafe](javascript:alert(1))", local::add) }
            }
        }
        onNodeWithText("https://example.org").clickFirstGlyph()
        onNodeWithText("Guide").clickFirstGlyph()
        onNodeWithText("File URI").clickFirstGlyph()
        onNodeWithText("Source line").clickFirstGlyph()
        onNodeWithText("Unsafe").clickFirstGlyph()
        runOnIdle {
            assertEquals(listOf("https://example.org"), web)
            assertEquals(listOf("guide.md", "file:///project/title.kt", "title.kt:12"), local)
        }
    }

    @Test
    fun inlineImageUsesTaskBasePathAndOpensTheResolvedLocalTarget() = runDesktopComposeUiTest {
        val requests = mutableListOf<Pair<String, String?>>()
        val opened = mutableListOf<String>()
        val loader = MarkdownImageLoader { source, base ->
            requests += source to base
            MarkdownImageLoadResult.Available(ImageBitmap(8, 4), "/project/images/preview.png")
        }
        setContent {
            MoruTheme {
                MarkdownReadingView("Before ![Mockup](images/preview.png) after", opened::add, imageBasePath = "/project", imageLoader = loader)
            }
        }
        waitUntil { requests.isNotEmpty() }
        waitForIdle()
        onNodeWithText("Before ").assertIsDisplayed()
        onNodeWithText(" after").assertIsDisplayed()
        onNodeWithContentDescription("Mockup").performClick()
        runOnIdle {
            assertEquals(listOf<Pair<String, String?>>("images/preview.png" to "/project"), requests.toList())
            assertEquals(listOf("/project/images/preview.png"), opened)
        }
    }

    @Test
    fun imageShowsLoadingThenUnavailableWithoutHidingTheResponse() = runDesktopComposeUiTest {
        val pending = CompletableDeferred<MarkdownImageLoadResult>()
        val loader = MarkdownImageLoader { _, _ -> pending.await() }
        setContent {
            MoruTheme { MarkdownReadingView("Useful text\n\n![Preview](missing.png)", {}, imageLoader = loader) }
        }
        onNodeWithTag("markdown-image-loading").assertIsDisplayed()
        onNodeWithText("Useful text").assertIsDisplayed()
        runOnIdle { pending.complete(MarkdownImageLoadResult.Unavailable) }
        waitForIdle()
        onNodeWithTag("markdown-image-unavailable").assertIsDisplayed()
        onNodeWithText("Preview").assertIsDisplayed()
        onNodeWithText("Useful text").assertIsDisplayed()
    }

    @Test
    fun imagePreviewFillsViewportAndSupportsZoomAndReset() = runDesktopComposeUiTest(width = 800, height = 640) {
        val loader = MarkdownImageLoader { _, _ ->
            MarkdownImageLoadResult.Available(ImageBitmap(64, 48), "/project/diagram.png")
        }
        setContent {
            MoruTheme {
                MarkdownImagePreview("/project/diagram.png", imageLoader = loader)
            }
        }
        waitForIdle()
        val imageBounds = onNodeWithTag("markdown-image").fetchSemanticsNode().boundsInRoot
        kotlin.test.assertTrue(imageBounds.width >= 780f, "Expected image to fill viewport width, was ${imageBounds.width}")
        kotlin.test.assertTrue(imageBounds.height >= 600f, "Expected image to fill viewport height beyond 360dp, was ${imageBounds.height}")
        onNodeWithText("100%").assertIsDisplayed()
        onNodeWithTag("markdown-image-zoom-in").performClick()
        onNodeWithText("125%").assertIsDisplayed()
        onNodeWithTag("markdown-image-zoom-in").performClick()
        onNodeWithText("150%").assertIsDisplayed()
        onNodeWithTag("markdown-image-zoom-out").performClick()
        onNodeWithText("125%").assertIsDisplayed()
        onNodeWithTag("markdown-image-zoom-reset").performClick()
        onNodeWithText("100%").assertIsDisplayed()
    }

    private class FakeClipboard : ClipboardManager {
        var held: AnnotatedString? = null
        override fun getText(): AnnotatedString? = held
        override fun setText(annotatedString: AnnotatedString) { held = annotatedString }
    }

    private fun SemanticsNodeInteraction.clickFirstGlyph() {
        val layouts = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val glyph = layouts.single().getBoundingBox(0)
        performTouchInput { click(glyph.center) }
    }
}
