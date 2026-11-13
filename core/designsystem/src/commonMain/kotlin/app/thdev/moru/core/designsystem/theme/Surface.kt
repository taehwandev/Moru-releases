package app.thdev.moru.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

enum class MoruSurfaceRole {
    Page,
    Panel,
    Card,
    CardSelected,
    /** A recessed container that groups related rows, such as a Project with its Tasks. */
    Group,
    GroupSelected,
    Row,
    RowSelected,
    Control,
    Code,
    Terminal,
    TerminalSelected,
    Empty,
    Warning,
    Success,
}

@Immutable
data class MoruSurfaceStyle(
    val background: Color,
    val content: Color,
    val border: Color,
    val borderWidth: Dp,
    val radius: Dp,
)

fun MoruSurfaceRole.resolveCkSurfaceStyle(
    colors: MoruColors,
    radius: MoruRadius = defaultCkRadius,
    stroke: MoruStroke = defaultCkStroke,
): MoruSurfaceStyle {
    return when (this) {
        MoruSurfaceRole.Page -> MoruSurfaceStyle(
            background = colors.background,
            content = colors.textPrimary,
            border = Color.Transparent,
            borderWidth = stroke.none,
            radius = radius.none,
        )

        MoruSurfaceRole.Panel -> MoruSurfaceStyle(
            background = colors.surface,
            content = colors.textPrimary,
            border = colors.border,
            borderWidth = stroke.thin,
            radius = radius.none,
        )

        MoruSurfaceRole.Card -> MoruSurfaceStyle(
            background = colors.surface,
            content = colors.textPrimary,
            border = colors.border,
            borderWidth = stroke.thin,
            radius = radius.none,
        )

        MoruSurfaceRole.CardSelected -> MoruSurfaceStyle(
            background = colors.primary.copy(alpha = if (colors.isLight) 0.08f else 0.14f),
            content = colors.textPrimary,
            border = colors.primary.copy(alpha = 0.45f),
            borderWidth = stroke.thin,
            radius = radius.medium,
        )

        MoruSurfaceRole.Group -> MoruSurfaceStyle(
            background = groupBackground(colors),
            content = colors.textPrimary,
            border = colors.border.copy(alpha = if (colors.isLight) 0.7f else 0.5f),
            borderWidth = stroke.thin,
            radius = radius.medium,
        )

        MoruSurfaceRole.GroupSelected -> MoruSurfaceStyle(
            background = groupBackground(colors),
            content = colors.textPrimary,
            border = colors.primary.copy(alpha = 0.45f),
            borderWidth = stroke.thin,
            radius = radius.medium,
        )

        MoruSurfaceRole.Row -> MoruSurfaceStyle(
            background = Color.Transparent,
            content = colors.textPrimary,
            border = Color.Transparent,
            borderWidth = stroke.none,
            radius = radius.none,
        )

        MoruSurfaceRole.RowSelected -> MoruSurfaceStyle(
            background = colors.primary.copy(alpha = if (colors.isLight) 0.07f else 0.22f),
            content = colors.textPrimary,
            border = Color.Transparent,
            borderWidth = stroke.none,
            radius = radius.none,
        )

        MoruSurfaceRole.Control -> MoruSurfaceStyle(
            background = colors.background,
            content = colors.textPrimary,
            border = colors.border,
            borderWidth = stroke.thin,
            radius = radius.small,
        )

        MoruSurfaceRole.Code -> MoruSurfaceStyle(
            background = colors.background,
            content = colors.textPrimary,
            border = colors.border,
            borderWidth = stroke.thin,
            radius = radius.none,
        )

        MoruSurfaceRole.Terminal -> MoruSurfaceStyle(
            background = colors.background,
            content = colors.textPrimary,
            border = colors.border,
            borderWidth = stroke.thin,
            radius = radius.none,
        )

        MoruSurfaceRole.TerminalSelected -> MoruSurfaceStyle(
            background = if (colors.isLight) colors.primary.copy(alpha = 0.08f) else colors.background,
            content = colors.textPrimary,
            border = colors.primary.copy(alpha = 0.45f),
            borderWidth = stroke.thin,
            radius = radius.none,
        )

        MoruSurfaceRole.Empty -> MoruSurfaceStyle(
            background = Color.Transparent,
            content = colors.textSecondary,
            border = colors.border,
            borderWidth = stroke.thin,
            radius = radius.medium,
        )

        MoruSurfaceRole.Warning -> MoruSurfaceStyle(
            background = colors.warning.copy(alpha = 0.08f),
            content = colors.warning,
            border = colors.warning.copy(alpha = 0.35f),
            borderWidth = stroke.thin,
            radius = radius.medium,
        )

        MoruSurfaceRole.Success -> MoruSurfaceStyle(
            background = colors.success.copy(alpha = 0.08f),
            content = colors.success,
            border = colors.success.copy(alpha = 0.35f),
            borderWidth = stroke.thin,
            radius = radius.medium,
        )
    }
}

private fun groupBackground(colors: MoruColors): Color =
    colors.background
