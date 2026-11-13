package app.thdev.moru.feature.workbench.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruStatusGlyph
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import kotlin.test.Test

/** Pins the status vocabulary: six shapes that stay distinguishable without their colour. */
class StatusGlyphScreenshotTest {
    private val comparator = GoldenImageComparator()

    private val labels = mapOf(
        MoruStatusGlyphKind.Working to "작업 중",
        MoruStatusGlyphKind.NeedsYou to "내 차례",
        MoruStatusGlyphKind.Unread to "읽지 않음",
        MoruStatusGlyphKind.Done to "완료",
        MoruStatusGlyphKind.Error to "실패",
        MoruStatusGlyphKind.Idle to "유휴",
    )

    @Test
    fun allKindsInLightTheme() = assertGlyphs("status_glyphs_light", lightCkColors)

    @Test
    fun allKindsInDarkTheme() = assertGlyphs("status_glyphs_dark", darkCkColors)

    private fun assertGlyphs(name: String, colors: MoruColors) =
        assertThemedScreenshot(comparator, name, colors, windowWidth = 440, windowHeight = 120) {
            Column(modifier = Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    labels.forEach { (kind, _) -> MoruStatusGlyph(kind = kind, size = 20.dp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    labels.forEach { (kind, label) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            MoruStatusGlyph(kind = kind)
                            MoruText(text = label, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
}
