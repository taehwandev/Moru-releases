package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A deep, long name in a narrow explorer is laid out whole and the tree grows wider than the pane to scroll to it. */
@OptIn(ExperimentalTestApi::class)
class ProjectTreeSidewaysScrollTest {
    private val longName = "CodePanelSourceThatHasAVeryLongDescriptiveName.kt"
    private val entries = listOf(
        WorkbenchProjectTreeEntry("feature", "feature", depth = 0, isFile = false),
        WorkbenchProjectTreeEntry("codepanel", "feature/codepanel", depth = 1, isFile = false),
        WorkbenchProjectTreeEntry("kotlin", "feature/codepanel/kotlin", depth = 2, isFile = false),
        WorkbenchProjectTreeEntry(longName, "feature/codepanel/kotlin/$longName", depth = 3, isFile = true),
    )

    @Test
    fun aLongDeepNameIsNotCutShortInANarrowPane() = runDesktopComposeUiTest {
        setContent {
            MoruTheme {
                ProjectWorktreePane(
                    workspacePath = "/work/alpha",
                    entries = entries,
                    selectedFilePath = null,
                    onOpenFile = {},
                    gitStatus = WorkbenchGitStatus.NotRepository,
                    onRefreshGitStatus = {},
                    expandedFolders = setOf("feature", "feature/codepanel", "feature/codepanel/kotlin"),
                    modifier = Modifier.width(200.dp).height(400.dp),
                )
            }
        }
        val node = onNodeWithText(longName)
        val layouts = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        val layout = layouts.single()
        assertFalse(layout.hasVisualOverflow, "the name must fit its row instead of being clipped")
        // The name alone, at its indent, is wider than the pane: the tree is laid out wider and scrolls to it.
        val nameWidth = with(density) { layout.size.width.toDp() }
        assertTrue(nameWidth + TreeStart + TreeIndent * 3 > 200.dp, "the fixture name must not fit the pane")
        assertTrue(node.fetchSemanticsNode().size.width >= layout.size.width, "the name's cell holds its full width")    }
}
