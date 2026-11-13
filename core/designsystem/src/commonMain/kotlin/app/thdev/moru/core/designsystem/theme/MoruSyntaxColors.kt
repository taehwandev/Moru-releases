package app.thdev.moru.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.thdev.moru.core.model.code.CodeSyntaxPalette

/** The colors code is drawn in, one per kind of token, tuned for the light or dark theme the app shows. */
@Immutable
data class MoruSyntaxColors(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val annotation: Color,
    val type: Color,
    val function: Color,
) {
    companion object {
        val Light = from(CodeSyntaxPalette.Light)
        val Dark = from(CodeSyntaxPalette.Dark)

        /** The syntax palette that matches [colors]. */
        fun of(colors: MoruColors): MoruSyntaxColors = if (colors.isLight) Light else Dark

        private fun from(palette: CodeSyntaxPalette): MoruSyntaxColors {
            fun color(hex: String) = Color(0xFF000000L or hex.removePrefix("#").toLong(16))
            return MoruSyntaxColors(color(palette.keyword), color(palette.string), color(palette.number), color(palette.comment),
                color(palette.annotation), color(palette.type), color(palette.function))
        }
    }
}
