package app.thdev.moru.app

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AboutMoruInteractionTest {
    @Test
    fun installedAndDevelopmentVersionsUseTheSameDisplayRule() {
        assertEquals("Moru 26.41.3", appVersionLabel("26.41.3"))
        assertEquals("Moru ${MoruBuildVersion.value} · Dev", appVersionLabel(null))
        assertEquals(appVersionLabel(null), appVersionLabel(" "))
        assertTrue(MoruBuildVersion.value.isNotBlank())
    }
}
