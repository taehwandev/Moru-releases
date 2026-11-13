package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.panel.PanelAxis
import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.core.model.panel.PanelSlot
import app.thdev.moru.core.model.panel.PanelSplit
import kotlin.test.Test
import kotlin.test.assertEquals

/** Closing a terminal collapses its split; the remaining panes move out of it without the app crashing or reloading. */
@OptIn(ExperimentalTestApi::class)
class TerminalPanelTreeCloseTest {
    private fun closing(start: PanelNode, keys: List<String>, after: PanelNode, kept: List<String>, keysFirst: Boolean) =
        runDesktopComposeUiTest(width = 900, height = 500) {
            var root: PanelNode by mutableStateOf(start)
            var shown by mutableStateOf(keys)
            val created = mutableMapOf<String, Int>()
            setContent {
                MoruTheme {
                    TerminalPanelTree(root, shown, onChange = { root = it }, modifier = Modifier.fillMaxSize()) { index, cellModifier ->
                        val key = shown.getOrNull(index) ?: "none"
                        remember { created[key] = (created[key] ?: 0) + 1 }
                        Box(cellModifier) { Text("pane $key") }
                    }
                }
            }
            waitForIdle()
            when (keysFirst) {
                true -> { shown = kept; waitForIdle(); root = after }
                false -> { shown = kept; root = after }
            }
            waitForIdle()
            kept.forEach { key ->
                onNodeWithText("pane $key").assertExists()
                assertEquals(1, created[key], "$key keeps its composition")
            }
        }

    private val two = PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSlot(1)))
    private val three = PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSplit(PanelAxis.Column, listOf(PanelSlot(1), PanelSlot(2)))))

    @Test fun closingOneOfTwoPanes() = closing(two, listOf("agy", "claude"), PanelSlot(0), listOf("claude"), false)
    @Test fun closingOneOfTwoPanesKeysFirst() = closing(two, listOf("agy", "claude"), PanelSlot(0), listOf("claude"), true)

    @Test fun closingANestedPane() = closing(three, listOf("codex", "agy", "claude"),
        PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSlot(1))), listOf("codex", "claude"), false)
}
