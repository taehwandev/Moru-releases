package app.thdev.moru.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MoruIconTest {
    @Test
    fun everyIconNameDrawsSomethingInTheColorItWasGiven() {
        MoruIconName.entries.forEach { name ->
            runDesktopComposeUiTest(width = 48, height = 48) {
                setContent { MoruIcon(name = name, size = 32.dp, tint = Color(0xFFE53935)) }
                val image = onRoot().captureToImage()
                val pixels = IntArray(image.width * image.height)
                image.readPixels(pixels)
                val red = pixels.count { argb -> (argb ushr 24) > 0 && ((argb shr 16) and 0xFF) > 0xC0 && (argb and 0xFF) < 0x80 }
                assertTrue(red > 15, "$name drew no pixels in the tint color ($red)")
            }
        }
    }
}
