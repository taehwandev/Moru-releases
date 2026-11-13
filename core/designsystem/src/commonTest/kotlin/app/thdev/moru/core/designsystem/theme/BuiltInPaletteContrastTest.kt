package app.thdev.moru.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class BuiltInPaletteContrastTest {
    private val palettes = mapOf(
        "Light" to lightCkColors, "Dark" to darkCkColors,
        "Fresh Mint" to freshMintCkColors, "GitHub Light" to githubLightCkColors,
        "GitHub Dark" to githubDarkCkColors, "Catppuccin Latte" to catppuccinLatteCkColors,
        "Catppuccin Mocha" to catppuccinMochaCkColors, "Tokyo Night" to tokyoNightCkColors,
    )

    @Test
    fun smallTextAndStatusColorsStayReadableOnOpaqueAndTintedSurfaces() {
        palettes.forEach { (name, colors) ->
            listOf(colors.background, colors.surface).forEach { base ->
                listOf(colors.textPrimary, colors.textSecondary, colors.primary,
                    colors.error, colors.success, colors.warning).forEach { foreground ->
                    assertContrast(name, foreground, base)
                }
                listOf(MoruSurfaceRole.Warning, MoruSurfaceRole.Success).forEach { role ->
                    val style = role.resolveCkSurfaceStyle(colors)
                    assertContrast("$name $role", style.content, style.background.compositeOver(base))
                }
                assertContrast("$name button", colors.onPrimary, colors.primary)
                listOf(MoruSurfaceRole.Code, MoruSurfaceRole.Group).forEach { role ->
                    val style = role.resolveCkSurfaceStyle(colors)
                    assertContrast("$name $role", style.content, style.background)
                }
            }
        }
    }

    private fun assertContrast(name: String, foreground: Color, background: Color) {
        val a = foreground.luminance()
        val b = background.luminance()
        val ratio = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
        assertTrue(ratio >= 4.5f, "$name: $foreground on $background has contrast $ratio; needs 4.5:1")
    }
}
