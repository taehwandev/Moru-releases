package app.thdev.moru.feature.settings.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.settings.i18n.onboardingStrings
import androidx.compose.foundation.shape.CircleShape

/** A short row of thin bars, the pages done and the current one filled; the page's name is read out, not printed. */
@Composable
internal fun OnboardingStepIndicator(steps: List<OnboardingStep>, current: Int, modifier: Modifier = Modifier) {
    val strings = onboardingStrings()
    val colors = MoruTheme.colors
    Row(
        modifier = modifier.semantics { contentDescription = "${current + 1} / ${steps.size} · ${strings.stepTitle(steps[current])}" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        steps.indices.forEach { at ->
            val fill by animateColorAsState(if (at <= current) colors.primary else colors.border, label = "onboarding-bar")
            Box(Modifier.width(36.dp).height(3.dp).background(fill, CircleShape))
        }
    }
}
