package app.thdev.moru.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Entity identity, independent of success, warning, error, or execution state. */
enum class MoruIdentityAccent {
    First, Second, Third, Fourth, Fifth, Sixth;

    fun color(colors: MoruColors): Color = if (colors.isLight) {
        when (this) {
            First -> Color(0xFF386BBA)
            Second -> Color(0xFF8653B4)
            Third -> Color(0xFFB34F7B)
            Fourth -> Color(0xFFAE7227)
            Fifth -> Color(0xFF287C78)
            Sixth -> Color(0xFF46798D)
        }
    } else {
        when (this) {
            First -> Color(0xFF8DB6F4)
            Second -> Color(0xFFBEA0ED)
            Third -> Color(0xFFE69AB9)
            Fourth -> Color(0xFFDBB577)
            Fifth -> Color(0xFF83C8C0)
            Sixth -> Color(0xFF8DBECF)
        }
    }
}
