package app.thdev.moru.feature.workbench.resume

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class RememberAgentExitsTest {
    /**
     * Coming back to a Task composes the watch before its first poll. Reporting "no agent running" for that moment
     * showed the resume bar above the terminal and took it away again, a resize that broke a running Claude Code.
     */
    @Test
    fun whetherAnAgentRunsIsUnknownUntilTheFirstReading() = runDesktopComposeUiTest {
        val sessions = InMemoryTerminalSessionManager()
        val seen = mutableListOf<Boolean?>()
        setContent { seen += rememberAgentExits(sessions, "task-1").anyAgentRunning }

        waitUntil { seen.lastOrNull() != null }
        assertNull(seen.first(), "the first composition claimed to know: $seen")
        assertEquals(false, seen.last())
    }
}
