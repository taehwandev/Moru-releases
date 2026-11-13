package app.thdev.moru.feature.workbench.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruMenuItem
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pins the glass menu every Moru dropdown shares: a long branch list (as in the worktree picker) stays a sheet of
 * bounded height with the chosen line checked, instead of Material's default menu. The menu opens as its own popup
 * root, which is what is captured.
 */
@OptIn(ExperimentalTestApi::class)
class GlassMenuScreenshotTest {
    private val comparator = GoldenImageComparator()

    private val branches = listOf(
        "feature/DHL-31050-avoid-tolls-option-update · project folder",
        "validation/DHL-31998-success",
        "validation/DHL-31998-real-api",
        "fix/mpb-promo-tag-import",
        "tada-release-rotation-next-20261006",
        "fix/next-android-release-manager",
        "tada-release-rotation-next-validation-20261006",
        "feature/DHL-30939-update-backmerge-mechanism",
        "tada-android-DHL-31050",
        "feature/DHL-31809-floating-gnb-tab-components",
        "feature/DHL-31998-release-rotation-metadata",
        "feature/DHL-31998-release-rotation-develop",
        "experiment/glass-gnb",
    )

    @Test
    fun longMenuInLightTheme() = assertMenu("glass_menu_light", lightCkColors)

    @Test
    fun longMenuInDarkTheme() = assertMenu("glass_menu_dark", darkCkColors)

    private fun assertMenu(name: String, colors: MoruColors) = runDesktopComposeUiTest(width = 480, height = 560) {
        setContent {
            MoruTheme(darkTheme = !colors.isLight, colors = colors, materialStyle = MoruMaterialStyle.Apple) {
                Box(Modifier.size(480.dp, 560.dp).background(colors.background).padding(24.dp)) {
                    MoruText("Worktree")
                    MoruDropdownMenu(
                        expanded = true,
                        onDismiss = {},
                        modifier = Modifier.testTag("glass-menu"),
                        items = branches.mapIndexed { index, branch -> MoruMenuItem(branch, checked = index == 0) {} },
                    )
                }
            }
        }
        val menu = onNodeWithTag("glass-menu")
        val bounds = menu.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width <= 360f && bounds.height <= 360f, "Long menus must keep their bounded viewport: $bounds")
        comparator.assertMatches(name, menu.captureToImage().toAwtImage())
    }
}
