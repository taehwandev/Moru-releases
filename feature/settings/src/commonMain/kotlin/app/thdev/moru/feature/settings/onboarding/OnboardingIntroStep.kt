package app.thdev.moru.feature.settings.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.feature.settings.i18n.onboardingStrings

/** Three lines on what Moru is. */
@Composable
internal fun OnboardingIntroStep() {
    val strings = onboardingStrings()
    OnboardingPageHeader(strings.introTitle, *strings.introBody.toTypedArray())
}
