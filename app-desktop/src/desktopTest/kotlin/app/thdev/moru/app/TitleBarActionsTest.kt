package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** The notification bell sits in the title bar, and the inbox always fits its window. */
@OptIn(ExperimentalTestApi::class)
class TitleBarActionsTest {
    @Test
    fun theInboxKeepsItsWidthInARoomyWindowAndShrinksToFitASmallOne() {
        assertEquals(380.dp to 640.dp, notificationInboxSize(28.dp, 1600.dp, 1000.dp))
        val (width, height) = notificationInboxSize(28.dp, 360.dp, 420.dp)
        assertEquals(360.dp - 36.dp, width)
        assertEquals(420.dp - 28.dp - 36.dp, height)
    }
}
