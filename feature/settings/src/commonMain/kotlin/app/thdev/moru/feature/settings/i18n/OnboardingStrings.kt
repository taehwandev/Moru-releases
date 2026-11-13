package app.thdev.moru.feature.settings.i18n

import app.thdev.moru.feature.settings.onboarding.OnboardingStep

/** The first-run welcome's text. */
interface OnboardingStrings {
    fun stepTitle(step: OnboardingStep): String
    val back: String
    val next: String
    val skipAll: String
    val start: String
    val finish: String

    val introTitle: String
    val introBody: List<String>

    val themeTitle: String
    val themeBody: String
    val appleStyleTitle: String
    val appleStyleBody: String
    val doneTitle: String
    val doneBody: String
    val openProject: String
}
