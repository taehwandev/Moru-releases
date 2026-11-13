package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruGlassRole
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** A sheet without a native backdrop must have the same pixels over light and dark backing. */
@OptIn(ExperimentalTestApi::class)
class MoruPopoverBackingTest {
    @Test
    fun unbackedAppleSheetsDoNotBlendWithWhiteOrBlackBehindThePopup() = runDesktopComposeUiTest(width = 160, height = 80) {
        setContent {
            MoruTheme(darkTheme = true, materialStyle = MoruMaterialStyle.Apple) {
                Row {
                    listOf("white" to Color.White, "black" to Color.Black).forEach { (tag, backing) ->
                        Box(Modifier.size(80.dp).background(backing)) {
                            MoruPopoverMaterial {
                                Box(Modifier.fillMaxSize().moruGlassSurface(MoruGlassRole.Sheet, shadow = false).testTag(tag))
                            }
                        }
                    }
                }
            }
        }
        val white = onNodeWithTag("white").captureToImage().toPixelMap()[40, 40]
        val black = onNodeWithTag("black").captureToImage().toPixelMap()[40, 40]
        assertTrue(abs(white.red - black.red) < .01f && abs(white.green - black.green) < .01f &&
            abs(white.blue - black.blue) < .01f, "Popup background leaked through the sheet: $white vs $black")
    }
}
