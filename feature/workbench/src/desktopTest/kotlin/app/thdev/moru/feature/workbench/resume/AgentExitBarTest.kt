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

@OptIn(ExperimentalTestApi::class)
class AgentExitBarTest {
    @Test
    fun theBarNamesTheAgentAndOffersToResumeHereOrToLeaveIt() = runDesktopComposeUiTest {
        val actions = mutableListOf<String>()
        setContent {
            MoruTheme {
                AgentExitBar(
                    exit = AgentExit("s1", AgentRuntime.Claude, "claude --resume c1"),
                    onResume = { actions += "resume" },
                    onDismiss = { actions += "dismiss" },
                    modifier = Modifier.width(560.dp),
                )
            }
        }

        onNodeWithText("Claude has quit.").assertExists()
        onNodeWithText("Resume here").performClick()
        onNodeWithText("Dismiss").performClick()

        runOnIdle { assertEquals(listOf("resume", "dismiss"), actions) }
    }

    @Test
    fun anExitWithoutAKnownConversationOnlyOffersDismiss() = runDesktopComposeUiTest {
        val actions = mutableListOf<String>()
        setContent {
            MoruTheme {
                AgentExitBar(
                    exit = AgentExit("s1", AgentRuntime.Codex),
                    onResume = { actions += "resume" },
                    onDismiss = { actions += "dismiss" },
                    modifier = Modifier.width(560.dp),
                )
            }
        }

        onNodeWithText("Codex has quit.").assertExists()
        onNodeWithText("Resume here").assertDoesNotExist()
        onNodeWithText("Dismiss").performClick()
        runOnIdle { assertEquals(listOf("dismiss"), actions) }
    }
}
