package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Set MORU_MARKDOWN_SHOT_DIR to retain the same native reader fixtures for visual inspection. */
@OptIn(ExperimentalTestApi::class)
class MarkdownCalloutReadingTest {
    private val examples = """
        > [!NOTE]
        > Keep this useful context beside the result.

        > [!TIP]
        > A small suggestion for the next task.

        > [!IMPORTANT]
        > This detail needs attention.

        > [!WARNING]
        > Review the changes before publishing.

        > [!CAUTION]
        > This action removes the selected data.

        > [!AI]
        > The assistant's explanation stays easy to scan.

        > [!USER]
        > Could you improve the reading view?

        > [!BOT]
        > The preview now uses the same callout language.

        > [!STEP]
        > Check the result
        >
        > Open Preview and read the complete response.
    """.trimIndent()

    @Test
    fun everyKindHasAReadableLabelAndBodyInLightAndDark() {
        listOf(false, true).forEach { dark ->
            draw("callouts-${if (dark) "dark" else "light"}", examples, dark, width = 760, height = 1_060) {
                listOf("Note", "Tip", "Important", "Warning", "Caution", "AI insight", "User question", "AI answer", "Step").forEach {
                    onNodeWithText(it).assertIsDisplayed()
                }
                listOf(
                    "Keep this useful context beside the result.",
                    "A small suggestion for the next task.",
                    "This detail needs attention.",
                    "Review the changes before publishing.",
                    "This action removes the selected data.",
                    "The assistant's explanation stays easy to scan.",
                    "Could you improve the reading view?",
                    "The preview now uses the same callout language.",
                    "Check the result",
                    "Open Preview and read the complete response.",
                ).forEach { onNodeWithText(it).assertIsDisplayed() }
                onAllNodesWithText("Check the result").assertCountEquals(1)
                onNodeWithText("[!STEP]").assertDoesNotExist()
            }
        }
    }

    @Test
    fun narrowConversationWrapsLongKoreanTextAndKeepsStepNumbersAcrossKinds() {
        val stepTitle = "다음 메시지에서 결과를 확인하고 필요한 변경 사항을 자세하게 설명해 주세요."
        val userMessage = "키플로우 문서에서 사용하던 코멘트와 단계 안내가 View에서도 자연스럽게 보이면 좋겠어요. 긴 문장도 잘리지 않고 읽을 수 있게 개선해 주세요."
        val source = """
            > [!STEP]
            > $stepTitle
            >
            > Preview keeps the complete instruction visible.

            > [!USER]
            > $userMessage

            > [!BOT]
            > 확인했습니다. 문장을 읽기 편한 대화 형태로 표시합니다.

            > [!NOTE]
            > The note does not restart the sequence.

            > [!STEP]
            > Continue with the next task
        """.trimIndent()
        listOf(false, true).forEach { dark ->
            draw("callouts-narrow-${if (dark) "dark" else "light"}", source, dark, width = 320, height = 820) {
                onNodeWithText(stepTitle).assertIsDisplayed()
                onNodeWithText(userMessage).assertIsDisplayed()
                onAllNodesWithText(stepTitle).assertCountEquals(1)
                onNodeWithText("1").assertIsDisplayed()
                onNodeWithText("2").assertIsDisplayed()
                onNodeWithText("Continue with the next task").assertIsDisplayed()
                val user = onNodeWithTag("markdown-callout-user").getBoundsInRoot()
                val bot = onNodeWithTag("markdown-callout-bot").getBoundsInRoot()
                val title = onNodeWithText(stepTitle).getBoundsInRoot()
                val message = onNodeWithText(userMessage).getBoundsInRoot()
                assertTrue(user.left > bot.left && user.right > bot.right, "User and Bot keep opposite alignment")
                assertTrue(title.bottom - title.top > 20.dp && message.bottom - message.top > 20.dp, "Long text wraps into multiple lines")
                assertTrue(title.right <= 320.dp && message.right <= 320.dp, "Text stays inside the narrow pane")
            }
        }
    }

    @Test
    fun stepBadgeGrowsForLongSequenceNumbers() = runDesktopComposeUiTest(width = 320, height = 180) {
        setContent {
            MoruTheme {
                Column(Modifier.padding(16.dp)) {
                    MarkdownBlocks(
                        listOf(MarkdownBlock.Callout(MarkdownCalloutKind.Step, listOf(MarkdownBlock.Paragraph("A later step")), stepNumber = 12345)),
                        onOpenLink = {},
                        imageBasePath = null,
                        imageLoader = null,
                    )
                }
            }
        }
        onNodeWithText("12345").assertIsDisplayed()
        val number = onNodeWithText("12345").getBoundsInRoot()
        val badge = onNodeWithTag("markdown-callout-step-number").getBoundsInRoot()
        assertTrue(badge.right - badge.left >= number.right - number.left, "The complete step number fits in its badge")
    }

    @Test
    fun calloutLeadLinksImagesAndCustomCodeActionsKeepTheirCallbacks() = runDesktopComposeUiTest {
        val opened = mutableListOf<String>()
        val submitted = mutableListOf<String>()
        val code = "  println(\"Callout\")"
        val loader = MarkdownImageLoader { _, _ ->
            MarkdownImageLoadResult.Available(ImageBitmap(8, 4), "/project/preview.png")
        }
        val source = """
            > [!STEP]
            > [Guide](guide.md)

            > [!AI]
            > ```kotlin
            > $code
            > ```
            >
            > ![Callout preview](preview.png)
        """.trimIndent()
        setContent {
            MoruTheme {
                MarkdownReadingView(
                    content = source,
                    onOpenWikiLink = opened::add,
                    codeBlockAction = { text -> MoruButton("Use code", { submitted += text }) },
                    imageBasePath = "/project",
                    imageLoader = loader,
                )
            }
        }
        onNodeWithText("Guide").clickFirstGlyph()
        onNodeWithText("Use code").performClick()
        waitForIdle()
        onNodeWithContentDescription("Callout preview").performClick()
        runOnIdle {
            assertEquals(listOf("guide.md", "/project/preview.png"), opened)
            assertEquals(listOf(code), submitted)
        }
    }

    @Test
    fun codeInsideACalloutKeepsDefaultCopyAndFeedback() = runDesktopComposeUiTest {
        val clipboard = object : ClipboardManager {
            var held: AnnotatedString? = null
            override fun getText(): AnnotatedString? = held
            override fun setText(annotatedString: AnnotatedString) { held = annotatedString }
        }
        val code = "println(\"Nested code\")"
        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                MoruTheme { MarkdownReadingView("> [!TIP]\n> ```kotlin\n> $code\n> ```", {}) }
            }
        }
        onNodeWithText("Copy code").performClick()
        onNodeWithText("Copied").assertIsDisplayed()
        runOnIdle { assertEquals(code, clipboard.held?.text) }
    }

    private fun draw(name: String, source: String, dark: Boolean, width: Int, height: Int, check: ComposeUiTest.() -> Unit) =
        runDesktopComposeUiTest(width = width, height = height) {
            setContent {
                MoruTheme(darkTheme = dark) {
                    MarkdownReadingView(
                        source,
                        onOpenWikiLink = {},
                        modifier = Modifier.ckSurface(MoruSurfaceRole.Page).padding(16.dp),
                        padding = 0.dp,
                    )
                }
            }
            check()
            System.getenv("MORU_MARKDOWN_SHOT_DIR")?.let { folder ->
                waitForIdle()
                ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(folder, "$name.png"))
            }
        }

    private fun SemanticsNodeInteraction.clickFirstGlyph() {
        val layouts = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        performTouchInput { click(layouts.single().getBoundingBox(0).center) }
    }
}
