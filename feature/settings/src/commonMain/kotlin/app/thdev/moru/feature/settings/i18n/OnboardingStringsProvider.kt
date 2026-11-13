package app.thdev.moru.feature.settings.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

internal fun onboardingStrings(language: UiLanguage): OnboardingStrings =
    when (language) {
        UiLanguage.English -> EnglishOnboardingStrings
        UiLanguage.Korean -> KoreanOnboardingStrings
    }

@Composable
internal fun onboardingStrings(): OnboardingStrings = onboardingStrings(LocalUiLanguage.current)
