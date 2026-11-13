package app.thdev.moru.core.designsystem.component.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlinx.coroutines.launch

/**
 * A row of choices in one capsule, like the macOS segmented control: a translucent track and a softer pill that slides
 * under the selected choice. It draws the same in every theme and material style, so it needs no glass behind it.
 */
@Composable
fun <T> MoruSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = MoruTheme.colors
    val track = if (colors.isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.07f)
    val trackEdge = if (colors.isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f)
    val pill = if (colors.isLight) Color.White else Color.White.copy(alpha = 0.16f)
    val pillShade = if (colors.isLight) Color.Black.copy(alpha = 0.10f) else Color.Transparent
    val density = LocalDensity.current
    // Where each choice sits inside the row, so the pill can slide to the selected one.
    val bounds = remember { mutableStateMapOf<Int, Pair<Dp, Dp>>() }
    val target = bounds[options.indexOf(selected)]
    val pillX = remember { Animatable(0.dp, Dp.VectorConverter) }
    val pillWidth = remember { Animatable(0.dp, Dp.VectorConverter) }
    LaunchedEffect(target) {
        val (x, width) = target ?: return@LaunchedEffect
        // The first place is taken at once; later changes slide there.
        if (pillWidth.value == 0.dp) {
            pillX.snapTo(x)
            pillWidth.snapTo(width)
        } else {
            launch { pillX.animateTo(x, spring(stiffness = 700f)) }
            pillWidth.animateTo(width, spring(stiffness = 700f))
        }
    }
    Row(
        modifier
            .background(track, CircleShape)
            .border(1.dp, trackEdge, CircleShape)
            .drawBehind {
                val width = pillWidth.value.toPx()
                if (width <= 0f) return@drawBehind
                val inset = TrackPadding.toPx()
                val height = size.height - inset * 2
                val radius = CornerRadius(height / 2)
                val left = inset + pillX.value.toPx()
                // A faint edge under the pill reads as lift on light themes.
                drawRoundRect(pillShade, Offset(left, inset + 0.75.dp.toPx()), Size(width, height), radius)
                drawRoundRect(pill, Offset(left, inset), Size(width, height), radius)
            }
            .padding(TrackPadding)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            MoruText(
                text = label(option),
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = if (isSelected) colors.textPrimary else colors.textPrimary.copy(alpha = 0.72f),
                modifier = Modifier
                    .onPlaced { placed ->
                        bounds[index] = with(density) { placed.positionInParent().x.toDp() to placed.size.width.toDp() }
                    }
                    .selectable(isSelected, role = Role.Tab, onClick = { onSelect(option) })
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }
    }
}

private val TrackPadding = 3.dp
