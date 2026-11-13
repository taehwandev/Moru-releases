package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MarkdownEditingEssentialsTest {
    @Test
    fun listKeyboardInputContinuesIndentsAndUndoesWithoutSaving() = runDesktopComposeUiTest {
        var draft by mutableStateOf("- [x] 한글")
        var saves = 0
        setContent { MoruTheme { Box(Modifier.size(760.dp, 480.dp)) { MarkdownLiveEditor(draft, { draft = it }, { saves++ }) } } }
        val editor = onNodeWithTag("markdown-live-editor")
        editor.performClick().performTextInputSelection(TextRange(draft.length))
        editor.performKeyInput { pressKey(Key.Enter) }
        runOnIdle { assertEquals("- [x] 한글\n- [ ] ", draft) }
        editor.performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.Z); keyUp(Key.CtrlLeft) }
        runOnIdle { assertEquals("- [x] 한글", draft) }
        editor.performKeyInput { pressKey(Key.Tab) }
        runOnIdle { assertEquals("  - [x] 한글", draft) }
        editor.performKeyInput { keyDown(Key.ShiftLeft); pressKey(Key.Tab); keyUp(Key.ShiftLeft) }
        runOnIdle { assertEquals("- [x] 한글", draft); assertEquals(0, saves) }
    }

    @Test
    fun findSeedsSelectionNavigatesBothWaysAndEscapeRestoresFocus() = runDesktopComposeUiTest {
        var draft by mutableStateOf("One two ONE")
        setContent { MoruTheme { Box(Modifier.size(760.dp, 500.dp)) { MarkdownLiveEditor(draft, { draft = it }, {}) } } }
        val editor = onNodeWithTag("markdown-live-editor")
        editor.performClick().performTextInputSelection(TextRange(0, 3))
        editor.performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.F); keyUp(Key.CtrlLeft) }
        onNodeWithTag("markdown-find").assertIsFocused()
        onNodeWithTag("markdown-match-count").assertTextEquals("1 / 2")
        onNodeWithTag("markdown-find").performKeyInput { pressKey(Key.Enter) }
        onNodeWithText("2 / 2").assertIsDisplayed()
        onNodeWithTag("markdown-find").performKeyInput { keyDown(Key.ShiftLeft); pressKey(Key.Enter); keyUp(Key.ShiftLeft) }
        onNodeWithText("1 / 2").assertIsDisplayed()
        onNodeWithText("Previous").performClick()
        onNodeWithText("2 / 2").assertIsDisplayed()
        onNodeWithTag("markdown-find").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("markdown-find").assertDoesNotExist()
        editor.assertIsFocused()
        runOnIdle { assertEquals("One two ONE", draft) }
    }

    @Test
    fun sourceModeReplaceAndReplaceAllAreSeparateUndoableEdits() = runDesktopComposeUiTest {
        var draft by mutableStateOf("**One**\n\nONE one")
        var saves = 0
        setContent { MoruTheme { Box(Modifier.size(760.dp, 540.dp)) { MarkdownLiveEditor(draft, { draft = it }, { saves++ }) } } }
        onNodeWithText("Source", substring = false).performClick()
        onNodeWithText("Find", substring = false).performClick()
        onNodeWithTag("markdown-find").performTextReplacement("one")
        onNodeWithTag("markdown-replacement").performTextReplacement("fixed")
        onNodeWithText("Replace", substring = false).performClick()
        runOnIdle { assertEquals("**fixed**\n\nONE one", draft) }
        onNodeWithText("Replace all", substring = false).performClick()
        runOnIdle { assertEquals("**fixed**\n\nfixed fixed", draft); assertEquals(0, saves) }
        onNodeWithTag("markdown-replacement").performKeyInput { pressKey(Key.Escape) }
        val editor = onNodeWithTag("markdown-live-editor")
        onNodeWithTag("markdown-find").assertDoesNotExist()
        editor.assertIsFocused()
        editor.performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.Z); keyUp(Key.CtrlLeft) }
        runOnIdle { assertEquals("**fixed**\n\nONE one", draft) }
        editor.performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.Z); keyUp(Key.CtrlLeft) }
        runOnIdle { assertEquals("**One**\n\nONE one", draft) }
    }

    @Test
    fun findScrollsToADistantSourceMatch() = runDesktopComposeUiTest {
        val source = (1..80).joinToString("\n") { "Paragraph $it" } + "\n**needle**\nlast line"
        var draft by mutableStateOf(source)
        setContent { MoruTheme { Box(Modifier.size(760.dp, 540.dp)) { MarkdownLiveEditor(draft, { draft = it }, {}) } } }
        onNodeWithText("Find", substring = false).performClick()
        onNodeWithTag("markdown-find").performTextReplacement("needle")
        onNodeWithTag("markdown-match-count").assertTextEquals("1 / 1")
        waitForIdle()
        val target = File("build/reports/markdown-essentials/distant-match.png").apply { parentFile.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", target)
        runOnIdle { assertEquals(source, draft) }
    }

    @Test
    fun narrowFindShowsEmptyAndNoMatchStatesInLightAndDark() = runDesktopComposeUiTest {
        var draft by mutableStateOf("# Notes\n\n한글 😀")
        var dark by mutableStateOf(false)
        setContent {
            MoruTheme(darkTheme = dark) {
                Box(Modifier.size(320.dp, 540.dp).background(MoruTheme.colors.surface)) {
                    MarkdownLiveEditor(draft, { draft = it }, {})
                }
            }
        }
        onNodeWithText("Find", substring = false).performClick()
        onNodeWithText("Type to find").assertIsDisplayed()
        onNodeWithTag("markdown-find").performTextReplacement("missing")
        onNodeWithText("No matches").assertIsDisplayed()
        for ((name, isDark) in listOf("light" to false, "dark" to true)) {
            runOnIdle { dark = isDark }
            waitForIdle()
            val target = File("build/reports/markdown-essentials/narrow-$name.png").apply { parentFile.mkdirs() }
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", target)
        }
        onNodeWithTag("markdown-find").performTextReplacement("")
        onNodeWithText("Type to find").assertIsDisplayed()
        runOnIdle { assertEquals("# Notes\n\n한글 😀", draft) }
    }
}
