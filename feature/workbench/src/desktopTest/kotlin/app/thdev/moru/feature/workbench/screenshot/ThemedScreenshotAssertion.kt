package app.thdev.moru.feature.workbench.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.UiLanguage

/**
 * Renders [content] under [colors] in a window of [windowWidth] x [windowHeight] px
 * and compares it with the golden named [name]. Content taller than the window is
 * clipped by the capture, so size the window to the content.
 */
@OptIn(ExperimentalTestApi::class)
internal fun assertThemedScreenshot(
    comparator: GoldenImageComparator,
    name: String,
    colors: MoruColors,
    windowWidth: Int = 1024,
    windowHeight: Int = 768,
    language: UiLanguage = UiLanguage.English,
    content: @Composable () -> Unit,
) = runDesktopComposeUiTest(width = windowWidth, height = windowHeight) {
    setContent {
        MoruTheme(darkTheme = !colors.isLight, colors = colors, materialStyle = MoruMaterialStyle.Apple) {
            CompositionLocalProvider(LocalUiLanguage provides language) {
                Box(Modifier.background(colors.background)) { content() }
            }
        }
    }
    comparator.assertMatches(name, onRoot().captureToImage().toAwtImage())
}
