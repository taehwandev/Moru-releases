package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruPanelCell
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** A device among a Task's terminals has a tab beside theirs, whose close button takes it out. */
@OptIn(ExperimentalTestApi::class)
class TerminalPanelTabTest {
    @Test
    fun aPanelAmongTheTerminalsHasATabThatClosesIt() = runDesktopComposeUiTest {
        var closed = 0
        val device = MoruPanelCell("device:android:emulator-5554", title = "Pixel 9 Pro Fold", onClose = { closed++ }) { m, _ -> Box(m) }
        setContent {
            MoruTheme {
                TerminalTabs(
                    sessions = emptyList(), selectedSessionId = null, canCreateSplit = true,
                    badgeType = { MoruBadgeType.NEUTRAL }, onSelectSession = {}, onNewTerminal = {}, panels = listOf(device),
                )
            }
        }
        onNodeWithText("Pixel 9 Pro Fold").assertExists()
        onNodeWithContentDescription("Close Pixel 9 Pro Fold").performClick()
        runOnIdle { assertEquals(1, closed) }
    }
}
