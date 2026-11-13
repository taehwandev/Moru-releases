package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.component.navigation.MoruSegmentedControl
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MoruSegmentedControlTest {
    @Test
    fun eachChoiceIsATabAndClickingOneSelectsIt() = runDesktopComposeUiTest {
        val picked = mutableListOf<String>()
        setContent {
            var selected by remember { mutableStateOf("Work") }
            MoruTheme(colors = darkCkColors) {
                MoruSegmentedControl(
                    options = listOf("Work", "Code", "Git"),
                    selected = selected,
                    onSelect = { selected = it; picked += it },
                    label = { it },
                )
            }
        }
        onNodeWithText("Work").assertIsSelected()
        onNodeWithText("Code").assertIsNotSelected().performClick()
        onNodeWithText("Code").assertIsSelected()
        onNodeWithText("Work").assertIsNotSelected()
        onNodeWithText("Git").performClick()
        runOnIdle { assertEquals(listOf("Code", "Git"), picked) }
    }
}
