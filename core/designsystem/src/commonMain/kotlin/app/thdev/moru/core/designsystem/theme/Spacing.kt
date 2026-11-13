package app.thdev.moru.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MoruSpacing(
    val none: Dp,
    val xSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val xLarge: Dp,
)

@Immutable
data class MoruRadius(
    val none: Dp,
    val xSmall: Dp,
    val small: Dp,
    val medium: Dp,
    /** Large containers such as dashboard cards. */
    val large: Dp,
    /** Standalone dialogs such as About. */
    val xLarge: Dp,
)

@Immutable
data class MoruStroke(
    val none: Dp,
    val thin: Dp,
)

val defaultCkSpacing = MoruSpacing(
    none = 0.dp,
    xSmall = 4.dp,
    small = 8.dp,
    medium = 12.dp,
    large = 16.dp,
    xLarge = 24.dp,
)

val defaultCkRadius = MoruRadius(
    none = 0.dp,
    xSmall = 4.dp,
    small = 6.dp,
    medium = 8.dp,
    large = 14.dp,
    xLarge = 24.dp,
)

val defaultCkStroke = MoruStroke(
    none = 0.dp,
    thin = 1.dp,
)

val LocalCkSpacing = staticCompositionLocalOf { defaultCkSpacing }
val LocalCkRadius = staticCompositionLocalOf { defaultCkRadius }
val LocalCkStroke = staticCompositionLocalOf { defaultCkStroke }
