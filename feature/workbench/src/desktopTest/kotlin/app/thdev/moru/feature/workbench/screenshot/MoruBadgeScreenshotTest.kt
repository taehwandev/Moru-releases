package app.thdev.moru.feature.workbench.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruBadge
import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import kotlin.test.Test

/**
 * Proves the screenshot harness end to end on the smallest real design-system
 * component before any Task UI depends on it.
 */
@OptIn(ExperimentalTestApi::class)
class MoruBadgeScreenshotTest {
    private val comparator = GoldenImageComparator()

    @Test
    fun badgesRenderInLightTheme() = assertBadgesMatch("badges_light", lightCkColors)

    @Test
    fun badgesRenderInDarkTheme() = assertBadgesMatch("badges_dark", darkCkColors)

    private fun assertBadgesMatch(name: String, colors: MoruColors) = runComposeUiTest {
        setContent {
            MoruTheme(darkTheme = !colors.isLight, colors = colors, materialStyle = MoruMaterialStyle.Apple) {
                Row(
                    modifier = Modifier.background(colors.background).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MoruBadgeType.entries.forEach { type -> MoruBadge(text = "승인 필요", type = type) }
                }
            }
        }
        comparator.assertMatches(name, onRoot().captureToImage().toAwtImage())
    }
}
