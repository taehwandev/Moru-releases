package app.thdev.moru.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTypographyTest {
    @Test
    fun ideSizeScalesTextAndLineHeightTogetherAndKeepsDefaultCodeMonospace() {
        val typography = appTypography(null, 28)
        assertEquals(28.sp, typography.body.fontSize)
        assertEquals(40.sp, typography.body.lineHeight)
        assertEquals(26.sp, typography.monospace.fontSize)
        assertEquals(FontFamily.Monospace, typography.monospace.fontFamily)
        assertEquals(FontFamily.Default, typography.body.fontFamily)
    }
}
