package app.thdev.moru.feature.workbench.resume

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.SessionThemeColors
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.workbench.TaskTerminalPane
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.slots.WorkbenchTerminalSlots
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalTestApi::class)
class TaskThemeRestartNoticeTest {
    @Test
    fun failedRestartStaysRetryableAndOnlyANewColorReadRemovesTheNotice() = runDesktopComposeUiTest {
        val sessions = InMemoryTerminalSessionManager()
        val session = sessions.createSession(TerminalSessionSpec(title = "Colors", attachedItemId = "task", startupCommand = "codex --no-daemon"))
        sessions.reportOutput(session.id, Clock.System.now().toEpochMilliseconds() - 60_000)
        SessionThemeColors.recordCanvas(session.id, 0x101010)
        SessionThemeColors.recordBackgroundColor(session.id, 0x101010)
        SessionThemeColors.recordCanvas(session.id, 0xf0f0f0)
        val typed = mutableListOf<String>()
        val slots = WorkbenchTerminalSlots(
            pane = { _, _ -> }, typeInto = { _, _ -> false }, submitLine = { _, line -> typed += line; true },
        )
        try {
            setContent {
                MoruTheme {
                    TaskTerminalPane(
                        sessions = sessions, folder = ".", shell = "/bin/sh", taskKey = "task", taskName = "Colors",
                        fontFamily = null, fontSize = 13, scrollbackLines = 100, onOpenExternalTerminal = {},
                        taskAgent = AgentRuntime.Codex, onStartSession = { null }, slots = slots, modifier = Modifier.width(700.dp),
                    )
                }
            }
            onNodeWithText("Restart for theme").assertExists()
            runOnIdle { assertTrue(typed.isEmpty(), "Theme observation cannot restart automatically") }
            repeat(2) {
                onNodeWithText("Restart for theme").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Codex could not restart safely. Try again when this session is idle.")
                        .fetchSemanticsNodes().isNotEmpty()
                }
                runOnIdle {
                    assertTrue(SessionThemeColors.hasStaleBackground(session.id))
                    assertTrue(typed.isEmpty(), "An unknown process must never receive a restart line")
                }
            }
            runOnIdle { SessionThemeColors.recordBackgroundColor(session.id, 0xf0f0f0) }
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Restart for theme").fetchSemanticsNodes().isEmpty() }
            runOnIdle {
                assertEquals(session.id, sessions.selectedSessionId())
                assertEquals("task", sessions.listSessions().single().spec.attachedItemId)
            }
        } finally {
            SessionThemeColors.forget(session.id)
        }
    }
}
