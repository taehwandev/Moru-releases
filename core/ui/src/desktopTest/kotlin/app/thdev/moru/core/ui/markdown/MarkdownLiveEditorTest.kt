package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.File
import javax.imageio.ImageIO

@OptIn(ExperimentalTestApi::class)
class MarkdownLiveEditorTest {
    @Test
    fun caretRevealsSyntaxInPlaceAndLightDarkNarrowLayoutsRender() = runDesktopComposeUiTest {
        val source = "# Live Markdown\n\n**Bold** and *italic* with [a link](guide.md)\n\n- [ ] Task\n\n```kotlin\nval greeting = \"한글 😀\"\n```\n"
        var draft by mutableStateOf(source)
        var dark by mutableStateOf(false)
        var width by mutableStateOf(760.dp)
        setContent {
            MoruTheme(darkTheme = dark) {
                Box(Modifier.size(width, 480.dp).background(MoruTheme.colors.surface)) {
                    MarkdownLiveEditor(draft, { draft = it }, {})
                }
            }
        }
        val editor = onNodeWithTag("markdown-live-editor")
        editor.performClick()
        editor.performTextInputSelection(TextRange(source.length))
        fun display(): String {
            val results = mutableListOf<TextLayoutResult>()
            editor.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            return results.single().layoutInput.text.text
        }
        assertEquals("Live Markdown\n\nBold and italic with a link\n\n- [ ] Task\n\n\nval greeting = \"한글 😀\"\n\n", display())
        for ((name, isDark, size) in listOf(Triple("light", false, 760.dp), Triple("dark", true, 760.dp), Triple("narrow", false, 320.dp))) {
            runOnIdle { dark = isDark; width = size }
            waitForIdle()
            val target = File("build/reports/markdown-live/$name.png").apply { parentFile.mkdirs() }
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", target)
        }
        editor.performTextInputSelection(TextRange(source.indexOf("Bold")))
        assertEquals(true, display().contains("**Bold** and *italic* with [a link](guide.md)"))
        runOnIdle { assertEquals(source, draft) }
    }

    @Test
    fun formattingUndoRedoAndSourceToggleNeverSaveImplicitly() = runDesktopComposeUiTest {
        var draft by mutableStateOf("hello\n\nworld")
        var saves = 0
        setContent { MoruTheme { Box(Modifier.size(760.dp, 480.dp)) { MarkdownLiveEditor(draft, { draft = it }, { saves++ }) } } }
        val editor = onNodeWithTag("markdown-live-editor")
        editor.performClick().performTextInputSelection(TextRange(0, 5))
        onNodeWithText("Bold").performClick()
        runOnIdle { assertEquals("**hello**\n\nworld", draft); assertEquals(0, saves) }
        onNodeWithText("Undo").performClick()
        runOnIdle { assertEquals("hello\n\nworld", draft) }
        onNodeWithText("Redo").performClick()
        onNodeWithText("Source", substring = false).performClick()
        onNodeWithText("Markdown source").assertIsDisplayed()
        editor.performTextInputSelection(TextRange(draft.length))
        editor.performTextInput("!")
        onNodeWithText("Live preview", substring = false).performClick()
        runOnIdle { assertEquals("**hello**\n\nworld!", draft); assertEquals(0, saves) }
        editor.performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.S); keyUp(Key.CtrlLeft) }
        runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun typingAndKeyboardFormattingWorkInAnEmptyNarrowEditor() = runDesktopComposeUiTest {
        var draft by mutableStateOf("")
        setContent { MoruTheme { Box(Modifier.size(320.dp, 360.dp)) { MarkdownLiveEditor(draft, { draft = it }, {}) } } }
        val editor = onNodeWithTag("markdown-live-editor")
        editor.performClick().performTextInput("한글 😀")
        editor.performTextInputSelection(TextRange(0, draft.length))
        editor.performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.B); keyUp(Key.CtrlLeft) }
        runOnIdle { assertEquals("**한글 😀**", draft) }
        editor.performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.Z); keyUp(Key.CtrlLeft) }
        runOnIdle { assertEquals("한글 😀", draft) }
    }

    @Test
    fun externalReloadIsShownAndClearsHistoryOfThePreviousSource() = runDesktopComposeUiTest {
        var draft by mutableStateOf("original")
        setContent { MoruTheme { Box(Modifier.size(760.dp, 480.dp)) { MarkdownLiveEditor(draft, { draft = it }, {}) } } }
        onNodeWithTag("markdown-live-editor").performClick().performTextInput("edit")
        runOnIdle { draft = "external reload" }
        waitForIdle()
        onNodeWithText("Undo").performClick()
        runOnIdle { assertEquals("external reload", draft) }
    }

    @Test
    fun reloadWhileFormattingButtonIsPressedNeverFormatsAnOlderDraft() = runDesktopComposeUiTest {
        var draft by mutableStateOf("original")
        setContent { MoruTheme { Box(Modifier.size(760.dp, 480.dp)) { MarkdownLiveEditor(draft, { draft = it }, {}) } } }
        val editor = onNodeWithTag("markdown-live-editor")
        editor.performClick().performTextInputSelection(TextRange(0, 8))
        onNodeWithText("Bold").performTouchInput { down(center) }
        runOnIdle { draft = "external reload" }
        waitForIdle()
        onNodeWithText("Bold").performTouchInput { up() }
        runOnIdle { assertEquals("external reload", draft.replace("**", "")) }
    }
}
