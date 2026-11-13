package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import java.awt.GraphicsEnvironment
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** The production menu must open readable About pixels above the heavyweight terminal surface. */
class AboutMoruNativeOsTest {
    @Test
    fun aboutOpensAboveSwingWithItsOwnReadableBackingAndCanBeReopened() {
        check(!GraphicsEnvironment.isHeadless())
        val variants = listOf(MoruMaterialStyle.Apple, MoruMaterialStyle.Standard)
            .flatMap { style -> listOf(true, false).map { dark -> style to dark } }
        variants.forEach { (style, dark) ->
            NativeWorkbenchTestWindow(900, 760, darkTheme = dark, materialStyle = style) {
                Column(Modifier.fillMaxSize().background(Color.Black)) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(40.dp)) {
                        TitleBarSettingsMenu("26.41.3", UpdateStatus.Idle, 0, {}, {}, {}, null, {})
                    }
                    SwingPanel(factory = { JPanel().apply { background = java.awt.Color.RED } },
                        background = Color.Red, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }.use { window ->
                repeat(3) { cycle ->
                    window.click("Settings menu")
                    window.click("About Moru")
                    window.waitFor("About content") { window.exists("A little calm for big ideas.") }
                    val image = window.captureOwnedSurface("about-native-${style.name.lowercase()}-${if (dark) "dark" else "light"}-$cycle")
                    assertTrue(image.width <= 420 && image.height <= 640,
                        "About must have its own compact native window, not an owner-sized layer: ${image.width}x${image.height}")
                    val expected = if (dark) darkCkColors.background else lightCkColors.background
                    var backgroundPixels = 0
                    for (y in 0 until image.height step 2) for (x in 0 until image.width step 2) {
                        val pixel = java.awt.Color(image.getRGB(x, y))
                        if (abs(pixel.red - expected.red * 255) < 12 &&
                            abs(pixel.green - expected.green * 255) < 12 && abs(pixel.blue - expected.blue * 255) < 12) backgroundPixels++
                    }
                    assertTrue(backgroundPixels > image.width * image.height / 10,
                        "About must paint its theme background above Swing, not an unpainted backing")
                    when (cycle) {
                        0 -> window.click("Close")
                        1 -> window.pressEscapeInOwnedSurface()
                        else -> window.clickBehindOwnedSurface()
                    }
                    window.waitFor("About closes") { !window.exists("A little calm for big ideas.") }
                }
            }
        }
    }
}
