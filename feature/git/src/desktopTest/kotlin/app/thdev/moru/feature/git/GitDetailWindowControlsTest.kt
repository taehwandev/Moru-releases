package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.git.components.navigation.GitDetailRestoreRail
import app.thdev.moru.feature.git.components.navigation.GitDetailWindowControls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** These headless tests exercise actions, not native window creation; the host has native lifecycle tests. */
@OptIn(ExperimentalTestApi::class)
class GitDetailWindowControlsTest {
    @Test
    fun detachAndRestoreUseDistinctAccessibleActions() = runDesktopComposeUiTest(width = 360, height = 200) {
        var detached by mutableStateOf(false)
        var detachRequests = 0
        var restoreRequests = 0
        setContent {
            MoruTheme {
                GitDetailWindowControls("Diff · 0123456", detached,
                    onDetach = { detachRequests++; detached = true },
                    onRestore = { restoreRequests++; detached = false })
            }
        }
        onNodeWithContentDescription("Detach Git detail pane").assertIsDisplayed().performClick()
        onNodeWithContentDescription("Detach Git detail pane").assertDoesNotExist()
        onNodeWithContentDescription("Restore Git detail pane").assertIsDisplayed().performClick()
        onNodeWithContentDescription("Detach Git detail pane").assertIsDisplayed()
        runOnIdle { assertEquals(1, detachRequests); assertEquals(1, restoreRequests) }
        captureGit("git-detail-window-controls")
    }

    @Test
    fun longLabelsLeaveTheCompactWindowActionVisible() = runDesktopComposeUiTest(width = 180, height = 180) {
        setContent {
            MoruTheme {
                GitDetailWindowControls("Changes in a project with a very long repository name", false,
                    onDetach = {}, onRestore = {})
            }
        }
        onNodeWithContentDescription("Detach Git detail pane").assertIsDisplayed().assertHasClickAction()
        val label = onNodeWithText("Changes in a project with a very long repository name").fetchSemanticsNode()
        val action = onNodeWithContentDescription("Detach Git detail pane").fetchSemanticsNode()
        runOnIdle { assertEquals(36f, action.boundsInRoot.width, 1f) }
        assertTrue(label.boundsInRoot.right <= action.boundsInRoot.left)
        captureGit("git-detail-window-controls-long")
    }

    @Test
    fun detachedMainRailRestoresWithoutAnyOtherAction() = runDesktopComposeUiTest(width = 80, height = 240) {
        var restores = 0
        setContent {
            MoruTheme {
                Column(Modifier.width(40.dp)) { GitDetailRestoreRail(onRestore = { restores++ }) }
            }
        }
        onNodeWithContentDescription("Restore Git detail pane").assertIsDisplayed().performClick()
        runOnIdle { assertEquals(1, restores) }
        captureGit("git-detail-restore-rail")
    }
}
