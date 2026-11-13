package app.thdev.moru.app

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AgentWorkflowKind
import app.thdev.moru.core.model.AgentWorkflowSettings
import app.thdev.moru.core.model.AgentWorkflowSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Knowledge tab picks folders with the system picker and lists Moru, then the user's own workflows, then the rest. */
@OptIn(ExperimentalTestApi::class)
class KnowledgeSourceListInteractionTest {
    private val sources = listOf(
        AgentWorkflowSource(AgentWorkflowKind.MoruCore, "/home/git/moru-core", found = true, skillCount = 3),
        AgentWorkflowSource(AgentWorkflowKind.Hermes, "/home/.hermes", found = false),
        AgentWorkflowSource(AgentWorkflowKind.ClaudeCode, "/home/.claude", found = true, skillCount = 2),
        AgentWorkflowSource(AgentWorkflowKind.Custom, "/home/flows", found = true, configuredRoot = "/home/flows"),
    )

    @Test
    fun sectionsRunFromMoruToTheUsersWorkflowsToWhatIsMissing() = runDesktopComposeUiTest(width = 900, height = 700) {
        setContent {
            MoruTheme {
                Column {
                    KnowledgeSourceList(sources, AgentWorkflowSettings(customRoots = listOf("/home/flows")), {}, { _, _ -> null })
                }
            }
        }
        val top = { text: String -> onNodeWithText(text).getBoundsInRoot().top }
        assertTrue(top("Moru Core") < top("Claude Code"))
        assertTrue(top("Claude Code") < top("Add folder…"))
        assertTrue(top("Add folder…") < top("UNAVAILABLE SOURCES"))
        assertTrue(top("UNAVAILABLE SOURCES") < top("Hermes Agent"))
    }

    @Test
    fun foldersComeFromThePickerAndCanBeRemoved() = runDesktopComposeUiTest(width = 900, height = 700) {
        var workflows by mutableStateOf(AgentWorkflowSettings(customRoots = listOf("/home/flows")))
        val picked = ArrayDeque(listOf("/elsewhere/moru", "/home/more"))
        setContent {
            MoruTheme {
                Column {
                    KnowledgeSourceList(sources, workflows, { workflows = it }, { _, _ -> picked.removeFirst() })
                }
            }
        }
        onNodeWithText("Change…").performClick()
        onNodeWithText("Add folder…").performClick()
        onNodeWithContentDescription("Remove /home/flows").performClick()
        waitForIdle()

        assertEquals(AgentWorkflowSettings(moruRoot = "/elsewhere/moru", customRoots = listOf("/home/more")), workflows)
    }
}
