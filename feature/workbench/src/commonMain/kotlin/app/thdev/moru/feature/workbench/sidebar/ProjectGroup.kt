package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDivider
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * One consistently framed Project and its indented Tasks or collapsed activity preview.
 * The active Project uses its identity accent without adding a second selection card or shifting its children.
 */
@Composable
internal fun ProjectGroup(
    first: Boolean,
    expanded: Boolean,
    selected: Boolean,
    active: Boolean = selected,
    accentColor: Color = MoruTheme.colors.primary,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit = {},
    collapsedContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colors = MoruTheme.colors
    val containerShape = RoundedCornerShape(MoruTheme.radius.medium)
    val targetHeaderBackground = if (selected) accentColor.copy(alpha = 0.08f) else Color.Transparent
    val targetContainerBackground = colors.surface
    val headerBackground by animateColorAsState(
        targetValue = targetHeaderBackground,
        animationSpec = tween(durationMillis = ExpandAnimationMillis, easing = FastOutSlowInEasing),
    )
    val containerBackground by animateColorAsState(
        targetValue = targetContainerBackground,
        animationSpec = tween(durationMillis = ExpandAnimationMillis, easing = FastOutSlowInEasing),
    )
    val accentProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = ExpandAnimationMillis, easing = FastOutSlowInEasing),
    )
    val contentReveal = remember { Animatable(1f) }
    var lastExpanded by remember { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        if (lastExpanded != expanded) {
            lastExpanded = expanded
            contentReveal.snapTo(0f)
            contentReveal.animateTo(1f, tween(durationMillis = ExpandAnimationMillis, easing = FastOutSlowInEasing))
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = GroupMargin, vertical = if (first) 4.dp else 6.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(containerShape)
                .background(containerBackground)
                .border(
                    width = MoruTheme.stroke.thin,
                    color = if (active || accentProgress > 0.01f) {
                        androidx.compose.ui.graphics.lerp(colors.border, accentColor.copy(alpha = 0.65f), accentProgress)
                    } else colors.border,
                    shape = containerShape,
                )
                .padding(bottom = 6.dp),
        ) {
            Box(
                Modifier.fillMaxWidth()
                    .background(headerBackground)
                    .padding(horizontal = HeaderPadding, vertical = 7.dp),
            ) {
                header()
            }
            if (expanded) {
                if (active) {
                    MoruDivider(
                        Modifier
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                            .alpha(if (colors.isLight) 0.5f else 0.38f),
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = TaskInset, end = TaskEndInset, top = if (active) 2.dp else 4.dp)
                        .graphicsLayer {
                            alpha = contentReveal.value
                            translationY = (1f - contentReveal.value) * (-RevealSlideDistance.toPx())
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    content()
                    footer()
                }
            } else {
                // No gap of its own, so a collapsed Project with nothing at work keeps its height.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = TaskInset, end = TaskEndInset)
                        .graphicsLayer {
                            alpha = contentReveal.value
                            translationY = (1f - contentReveal.value) * (-RevealSlideDistance.toPx())
                        },
                ) {
                    collapsedContent()
                }
            }
        }
    }
}

/** Keep the Project's clickable header clear of the sidebar edge. */
private val GroupMargin = 6.dp

private val HeaderPadding = 6.dp

/** A visible hierarchy step shared by selected and unselected Projects. */
private val TaskInset = 22.dp
private val TaskEndInset = 8.dp

private val RevealSlideDistance = 6.dp
private const val ExpandAnimationMillis = 210
