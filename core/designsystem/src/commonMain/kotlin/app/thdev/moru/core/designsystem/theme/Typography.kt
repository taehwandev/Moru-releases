package app.thdev.moru.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class MoruTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    /** Panel and section titles, between [h2] and [body]. */
    val title: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    /** Dense secondary rows: meta lines, inbox rows, chart legends. */
    val label: TextStyle,
    /** The smallest text: badges, axis ticks, count chips. */
    val micro: TextStyle,
    /** Large figures on dashboards. */
    val metric: TextStyle,
    /** The app's name or a dialog's hero title. */
    val display: TextStyle,
    /** A full page's centred title, such as an onboarding step: between [h1] and [display]. */
    val headline: TextStyle,
    val monospace: TextStyle
)

val defaultCkTypography = MoruTypography(
    h1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    title = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    label = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    micro = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 13.sp
    ),
    metric = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 30.sp
    ),
    display = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 44.sp
    ),
    headline = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    monospace = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
)

val LocalCkTypography = staticCompositionLocalOf { defaultCkTypography }
