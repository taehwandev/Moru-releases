package app.thdev.moru.feature.workbench.resume

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ResumeBarTest {
    @Test
    fun onlyAgentsWithATaskConversationHaveButtons() = runDesktopComposeUiTest {
        val resumed = mutableListOf<AgentRuntime>()
        setContent { MoruTheme { ResumeBar(runtimes = listOf(AgentRuntime.Claude, AgentRuntime.Claude), onResume = { resumed += it }, modifier = Modifier.width(520.dp)) } }

        onNodeWithText("Terminal").assertDoesNotExist()
        onNodeWithText("Codex").assertDoesNotExist()
        onNodeWithText("AGY").assertDoesNotExist()
        onNodeWithText("Claude").performClick()

        runOnIdle { assertEquals(listOf(AgentRuntime.Claude), resumed) }
    }

    @Test
    fun noTaskConversationLeavesNoResumeRow() = runDesktopComposeUiTest {
        setContent { MoruTheme { ResumeBar(runtimes = emptyList(), onResume = {}) } }

        onNodeWithText("Resume last session").assertDoesNotExist()
        onNodeWithText("Claude").assertDoesNotExist()
    }

    @Test
    fun theTasksOwnAgentComesFirst() = runDesktopComposeUiTest {
        setContent { MoruTheme { ResumeBar(runtimes = listOf(AgentRuntime.Claude, AgentRuntime.Codex, AgentRuntime.Agy), onResume = {}, modifier = Modifier.width(520.dp), preferred = AgentRuntime.Codex) } }

        val codex = onNodeWithText("Codex").fetchSemanticsNode().boundsInRoot.left
        val claude = onNodeWithText("Claude").fetchSemanticsNode().boundsInRoot.left
        val agy = onNodeWithText("AGY").fetchSemanticsNode().boundsInRoot.left

        assertTrue(codex < claude && codex < agy, "Codex should come first: $codex, $claude, $agy")
    }
}
