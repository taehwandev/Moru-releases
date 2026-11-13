package app.thdev.moru.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.knowledge.ReadOnlySourceTree
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class KnowledgeSourceTreeInteractionTest {
    @Test
    fun opensAndClosesNestedFoldersAndSelectsAFile() = runDesktopComposeUiTest(width = 280, height = 600) {
        var opened: String? = null
        setContent { MoruTheme { ReadOnlySourceTree("/moru", listOf("AGENTS.md", "skills/review/SKILL.md", "skills/guide.md"), null, "", { opened = it }) } }
        onNodeWithText("guide.md").assertDoesNotExist()
        onNodeWithText("skills").performClick()
        onNodeWithText("guide.md").assertExists()
        onNodeWithText("review").performClick()
        onNodeWithText("SKILL.md").performClick()
        assertEquals("skills/review/SKILL.md", opened)
        onNodeWithText("skills").performClick()
        onNodeWithText("SKILL.md").assertDoesNotExist()
        onNodeWithText("AGENTS.md").assertExists()
    }
}
