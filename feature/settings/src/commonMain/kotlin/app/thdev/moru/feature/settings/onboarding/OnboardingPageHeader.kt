package app.thdev.moru.feature.settings.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** A page's large centred title and the quieter lines under it. */
@Composable
internal fun OnboardingPageHeader(title: String, vararg lines: String) {
    val typography = MoruTheme.typography
    Column(
        modifier = Modifier.padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MoruText(title, style = typography.headline.copy(textAlign = TextAlign.Center))
        lines.forEach { line ->
            MoruText(line, style = typography.body.copy(textAlign = TextAlign.Center), color = MoruTheme.colors.textSecondary)
        }
    }
}
