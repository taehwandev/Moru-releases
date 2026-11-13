package app.thdev.moru.feature.settings.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.designsystem.theme.MacWindowControlColors

/**
 * One theme to pick, shown as a small Moru window in its colours; System shows light and dark split down the middle.
 * The picked one is outlined.
 */
@Composable
internal fun OnboardingThemeCard(theme: AppTheme, label: String, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MoruTheme.colors
    val shape = RoundedCornerShape(MoruTheme.radius.medium)
    val outline by animateColorAsState(if (selected) colors.primary else colors.border, label = "onboarding-theme-outline")
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Canvas(Modifier.size(width = 156.dp, height = 100.dp).clip(shape).border(if (selected) 2.dp else 1.dp, outline, shape)) {
            when (theme) {
                AppTheme.Dark -> drawMiniWindow(darkCkColors)
                AppTheme.System -> {
                    clipRect(right = size.width / 2) { drawMiniWindow(lightCkColors) }
                    clipRect(left = size.width / 2) { drawMiniWindow(darkCkColors) }
                }
                else -> drawMiniWindow(lightCkColors)
            }
        }
        MoruText(label, style = if (selected) MoruTheme.typography.title else MoruTheme.typography.body,
            color = if (selected) colors.textPrimary else colors.textSecondary)
    }
}

/** A title bar with the traffic lights, a sidebar of rows, and a page with a few lines and one amber "waiting" mark. */
private fun DrawScope.drawMiniWindow(palette: MoruColors) {
    val w = size.width
    val h = size.height
    val bar = h * 0.17f
    val unit = 1.dp.toPx()
    drawRect(palette.background)
    drawRect(palette.surface, size = Size(w, bar))
    MacWindowControlColors.forEachIndexed { at, light ->
        drawCircle(light, radius = 2.6f * unit, center = Offset(9f * unit + at * 8f * unit, bar / 2))
    }
    drawRect(palette.border, topLeft = Offset(0f, bar), size = Size(w, unit))
    val side = w * 0.3f
    drawRect(palette.surface, topLeft = Offset(0f, bar), size = Size(side, h - bar))
    drawRect(palette.border, topLeft = Offset(side, bar), size = Size(unit, h - bar))
    val line = palette.textSecondary.copy(alpha = 0.35f)
    val round = CornerRadius(2f * unit)
    repeat(4) { row ->
        drawRoundRect(line, topLeft = Offset(7f * unit, bar + (9f + row * 11f) * unit), size = Size(side - 14f * unit, 4f * unit), cornerRadius = round)
    }
    val left = side + 10f * unit
    val text = palette.textPrimary.copy(alpha = 0.22f)
    listOf(0.78f, 0.62f, 0.7f).forEachIndexed { row, length ->
        drawRoundRect(text, topLeft = Offset(left, bar + (10f + row * 11f) * unit), size = Size((w - left - 10f * unit) * length, 4f * unit), cornerRadius = round)
    }
    drawRoundRect(palette.warning, topLeft = Offset(left, h - 18f * unit), size = Size(26f * unit, 7f * unit), cornerRadius = CornerRadius(3.5f * unit))
}
