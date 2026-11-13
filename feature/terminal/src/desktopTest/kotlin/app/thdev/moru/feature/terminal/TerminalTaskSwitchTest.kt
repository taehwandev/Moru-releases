package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.panel.PanelAxis
import app.thdev.moru.core.model.panel.PanelLayouts
import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.core.model.panel.PanelSlot
import app.thdev.moru.core.model.panel.PanelSplit
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TerminalTaskSwitchTest {
    @Test
    fun switchingTasksWithTheSameSessionCountKeepsEachSavedArrangement() = preservesLayouts(3)

    @Test
    fun switchingTasksWithDifferentSessionCountsKeepsEachSavedArrangement() = preservesLayouts(2)

    private fun preservesLayouts(otherCount: Int) = runComposeUiTest {
        val sessions = InMemoryTerminalSessionManager()
        listOf("a" to 3, "b" to otherCount).forEach { (task, count) ->
            repeat(count) { index ->
                sessions.createSession(TerminalSessionSpec(
                    title = "$task-$index", attachedItemId = task, kind = TerminalSessionKind.LAB,
                ))
            }
        }
        val expected = mapOf<String, PanelNode>(
            "a" to PanelSplit(PanelAxis.Column, listOf(PanelSlot(2), PanelSlot(0), PanelSlot(1)), listOf(.2f, .5f, .3f)),
            "b" to PanelLayouts.swap(PanelLayouts.resize(PanelLayouts.grid(otherCount), emptyList(), 0, .7f), 0, 1),
        )
        val saved = mutableStateOf(expected)
        val task = mutableStateOf("a")
        setContent {
            MoruTheme {
                CompositionLocalProvider(LocalLabSessionPane provides { session, _, modifier ->
                    Box(modifier.testTag(session.title))
                }) {
                    Column(Modifier.size(1200.dp, 900.dp)) {
                        MultiTerminalPane(
                            sessionManager = sessions,
                            defaultWorkingDirectory = "/tmp",
                            defaultShell = "/bin/sh",
                            workspaceId = task.value,
                            workspaceName = task.value,
                            terminalFontFamily = null,
                            terminalFontSize = 13,
                            terminalScrollbackLines = 100,
                            split = saved.value[task.value],
                            onSplitChange = { saved.value = saved.value + (task.value to it) },
                        )
                    }
                }
            }
        }
        waitForIdle()
        runOnIdle { task.value = "b" }
        waitForIdle()
        assertEquals(expected["b"], saved.value["b"], "visiting B must not replace its saved tree")
        val left = onNodeWithTag("b-1").fetchSemanticsNode().boundsInRoot.left
        val right = onNodeWithTag("b-0").fetchSemanticsNode().boundsInRoot.left
        kotlin.test.assertTrue(left < right, "B must draw its saved reversed order")

        runOnIdle { task.value = "a" }
        waitForIdle()
        assertEquals(expected, saved.value, "returning to A must keep both Tasks' arrangements")
        val top = onNodeWithTag("a-2").fetchSemanticsNode().boundsInRoot.top
        val bottom = onNodeWithTag("a-1").fetchSemanticsNode().boundsInRoot.top
        kotlin.test.assertTrue(top < bottom, "A must keep its saved column order")
    }
}
