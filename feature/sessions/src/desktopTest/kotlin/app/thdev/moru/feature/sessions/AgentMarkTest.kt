package app.thdev.moru.feature.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.session.AgentRuntime
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AgentMarkTest {
    @Test
    fun providerAliasesResolveWithoutGuessingUnknownPrograms() {
        assertEquals(AgentMarkKind.Claude, AgentMarkKind.of("CLAUDE"))
        assertEquals(AgentMarkKind.Codex, AgentMarkKind.of("Codex"))
        listOf("AGY", "Antigravity", "Gemini").forEach { assertEquals(AgentMarkKind.Agy, AgentMarkKind.of(it)) }
        assertEquals(null, AgentMarkKind.of("aider"))
    }

    @Test
    fun everyProviderAndFallbackMarkUsesTheCurrentThemeForeground() {
        val custom = lightCkColors.copy(textPrimary = Color(0xFFAE1593), textSecondary = Color(0xFF006B82))
        listOf(lightCkColors, darkCkColors, custom).forEach { colors ->
            runDesktopComposeUiTest(width = 240, height = 48) {
                val runtimes = listOf(AgentRuntime.Named("Claude"), AgentRuntime.Named("Codex"),
                    AgentRuntime.Named("AGY"), AgentRuntime.Named("aider"), AgentRuntime.Terminal, AgentRuntime.WebPreview)
                setContent {
                    MoruTheme(colors = colors) {
                        Row(Modifier.background(colors.surface)) {
                            runtimes.forEachIndexed { index, agent -> AgentMark(agent, Modifier.testTag("mark:$index"), 24.dp) }
                        }
                    }
                }
                runtimes.indices.forEach { index ->
                    val pixels = onNodeWithTag("mark:$index").captureToImage().toPixelMap()
                    val expected = if (index < 3) colors.textPrimary else colors.textSecondary
                    val matches = (0 until pixels.height).sumOf { y ->
                        (0 until pixels.width).count { x -> closeTo(pixels[x, y], expected) }
                    }
                    assertTrue(matches > 2, "Provider $index must draw a visible shape using the current theme token")
                }
            }
        }
    }

    private fun closeTo(actual: Color, expected: Color): Boolean =
        abs(actual.red - expected.red) < .05f && abs(actual.green - expected.green) < .05f &&
            abs(actual.blue - expected.blue) < .05f && actual.alpha > .8f
}
