package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TerminalPanelCapacityTest {
    @Test
    fun fullTerminalCapacityStillOpensThePanelChooser() = checksChooser(true)

    @Test
    fun fullTerminalCapacityKeepsTheSessionOnlyButtonDisabled() = checksChooser(false)

    private fun checksChooser(canOpenPanels: Boolean) = runComposeUiTest {
        val sessions = InMemoryTerminalSessionManager()
        repeat(8) { sessions.createSession(TerminalSessionSpec(attachedItemId = "task", kind = TerminalSessionKind.LAB)) }
        var opened = 0
        setContent {
            MoruTheme {
                CompositionLocalProvider(LocalLabSessionPane provides { _, _, modifier -> Box(modifier) }) {
                    MultiTerminalPane(
                        sessionManager = sessions,
                        defaultWorkingDirectory = "/tmp",
                        defaultShell = "/bin/sh",
                        workspaceId = "task",
                        workspaceName = "Task",
                        terminalFontFamily = null,
                        terminalFontSize = 13,
                        terminalScrollbackLines = 100,
                        onNewTerminal = { opened++ },
                        canOpenPanels = canOpenPanels,
                        modifier = Modifier.size(1200.dp, 700.dp),
                    )
                }
            }
        }
        val button = onNodeWithContentDescription("New session")
        if (canOpenPanels) {
            button.assertIsEnabled().performClick()
            runOnIdle { assertEquals(1, opened) }
        } else {
            button.assertIsNotEnabled()
            runOnIdle { assertEquals(0, opened) }
        }
        runOnIdle { assertEquals(8, sessions.listSessions().size) }
    }
}
