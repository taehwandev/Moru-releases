package app.thdev.moru.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography as MaterialTypography

object MoruTheme {
    val materialStyle: MoruMaterialStyle
        @Composable
        get() = LocalMoruMaterialStyle.current

    val colors: MoruColors
        @Composable
        get() = LocalCkColors.current

    val typography: MoruTypography
        @Composable
        get() = LocalCkTypography.current

    val spacing: MoruSpacing
        @Composable
        get() = LocalCkSpacing.current

    val radius: MoruRadius
        @Composable
        get() = LocalCkRadius.current

    val stroke: MoruStroke
        @Composable
        get() = LocalCkStroke.current

    @Composable
    fun surfaceStyle(role: MoruSurfaceRole): MoruSurfaceStyle {
        return role.resolveCkSurfaceStyle(
            colors = LocalCkColors.current,
            radius = LocalCkRadius.current,
            stroke = LocalCkStroke.current,
        )
    }
}

@Composable
fun MoruTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: MoruColors = if (darkTheme) darkCkColors else lightCkColors,
    typography: MoruTypography = defaultCkTypography,
    materialStyle: MoruMaterialStyle = LocalMoruMaterialStyle.current,
    content: @Composable () -> Unit
) {
    val spacing = defaultCkSpacing
    val radius = defaultCkRadius
    val stroke = defaultCkStroke

    val materialColorScheme = if (!colors.isLight) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.background,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            error = colors.error,
            outline = colors.border
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.background,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            error = colors.error,
            outline = colors.border
        )
    }

    val materialTypography = MaterialTypography(
        headlineLarge = typography.h1,
        headlineMedium = typography.h2,
        bodyLarge = typography.body,
        bodyMedium = typography.body,
        labelMedium = typography.caption
    )

    CompositionLocalProvider(
        LocalCkColors provides colors,
        LocalCkTypography provides typography,
        LocalCkSpacing provides spacing,
        LocalCkRadius provides radius,
        LocalCkStroke provides stroke,
        LocalMoruMaterialStyle provides materialStyle,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = materialTypography,
            content = content
        )
    }
}
