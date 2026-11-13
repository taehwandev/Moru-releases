package app.thdev.moru.feature.git.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.history.GitGraphRow as ModelGraphRow
import app.thdev.moru.core.model.git.history.GitCommit
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GitGraphVisibleWidthTest {
    /** Forty recent one-lane commits over old history that once spread over twenty lanes. */
    private val rows = List(120) { index ->
        val lanes = if (index < 80) 1 else 20
        GitGraphRow(
            commit = GitCommit("%040d".format(index), emptyList(), "Dev", "dev@example.invalid", 0L, emptyList(), "Commit $index"),
            nodeLane = 0, nodeColor = 0, laneCount = lanes,
            through = (1 until lanes).map { ModelGraphRow.Lane(it, it) }, incoming = emptyList(), outgoing = emptyList(),
        )
    }

    @Test
    fun theGraphIsOnlyAsWideAsTheLanesOnScreen() = runDesktopComposeUiTest(width = 1000, height = 500) {
        setContent { MoruTheme { GitHistoryList(rows, selectedHash = null, nowEpochSeconds = 0L, onSelect = {}, Modifier.fillMaxSize()) } }
        waitForIdle()
        val narrow = onNodeWithTag("git-graph-column").getBoundsInRoot()
        assertTrue(narrow.right - narrow.left <= 60.dp, "one visible lane should keep the graph narrow: $narrow")

        onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Commit 110"))
        mainClock.advanceTimeBy(500)
        waitForIdle()
        val wide = onNodeWithTag("git-graph-column").getBoundsInRoot()
        assertTrue(wide.right - wide.left >= 200.dp, "twenty visible lanes should widen the graph: $wide")
        onNodeWithText("Commit 110").assertExists()
        assertTrue(onAllNodesWithTag("git-graph-column").fetchSemanticsNodes().size == 1)
    }
}
