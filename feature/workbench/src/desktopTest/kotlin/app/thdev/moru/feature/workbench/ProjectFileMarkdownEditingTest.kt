package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProjectFileMarkdownEditingTest {
    @Test
    fun readEditAndSourceModesShareTheDraftAndOnlySaveUpdatesBaseline() = runDesktopComposeUiTest {
        var state by mutableStateOf(WorkbenchEditorState.of(preview("hello\n")))
        var saved: String? = null
        setContent {
            MoruTheme {
                Box(Modifier.size(900.dp, 600.dp)) {
                    ProjectFilePreviewPane(
                        state, { state = state.withDraft(it) }, { saved = state.draft; state = state.markSaved() },
                        null, {}, { WorkbenchFileRenameOutcome.Failed("note.md", "unused") }, {},
                    )
                }
            }
        }
        onNodeWithText("Edit").performClick()
        val editor = onNodeWithTag("markdown-live-editor")
        editor.performClick().performTextInputSelection(TextRange(0, 5))
        onNodeWithText("Bold").performClick()
        runOnIdle { assertEquals("**hello**\n", state.draft); assertTrue(state.isDirty); assertEquals(null, saved) }
        onNodeWithText("Read", substring = false).performClick()
        onNodeWithText("hello", substring = false).assertIsDisplayed()
        onNodeWithText("Edit").performClick()
        onNodeWithText("Source", substring = false).performClick()
        onNodeWithTag("markdown-live-editor").performClick().performTextInputSelection(TextRange(state.draft.length))
        onNodeWithTag("markdown-live-editor").performTextInput("next")
        onNodeWithText("Save", substring = false).performClick()
        runOnIdle { assertEquals("**hello**\nnext", saved); assertFalse(state.isDirty) }
    }

    @Test
    fun truncatedMarkdownNeverOpensTheLiveEditor() = runDesktopComposeUiTest {
        val state = WorkbenchEditorState.of(preview("partial").copy(truncated = true, truncationText = "Truncated"))
        setContent {
            MoruTheme {
                Box(Modifier.size(900.dp, 600.dp)) {
                    ProjectFilePreviewPane(state, { error("read-only") }, { error("read-only") }, null, {},
                        { WorkbenchFileRenameOutcome.Failed("note.md", "unused") }, {})
                }
            }
        }
        onNodeWithText("Edit").performClick()
        onNodeWithTag("markdown-live-editor").assertDoesNotExist()
        onNodeWithText("Save", substring = false).assertDoesNotExist()
        onNodeWithText("Truncated").assertIsDisplayed()
    }

    private fun preview(content: String) = WorkbenchFilePreview(
        path = "note.md", name = "note.md", status = WorkbenchFilePreviewStatus.TEXT,
        sizeBytes = content.length.toLong(), content = content, summaryText = "Markdown",
    )
}
