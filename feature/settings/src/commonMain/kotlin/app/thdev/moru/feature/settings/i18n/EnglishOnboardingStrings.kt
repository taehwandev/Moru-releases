package app.thdev.moru.feature.settings.i18n

import app.thdev.moru.feature.settings.onboarding.OnboardingStep

internal object EnglishOnboardingStrings : OnboardingStrings {
    override fun stepTitle(step: OnboardingStep) = when (step) {
        OnboardingStep.Intro -> "Welcome"
        OnboardingStep.Theme -> "Look"
        OnboardingStep.Done -> "Ready"
    }
    override val back = "Back"
    override val next = "Next"
    override val skipAll = "Skip setup"
    override val start = "Get started"
    override val finish = "Start using Moru"

    override val introTitle = "Welcome to Moru"
    override val introBody = listOf(
        "Your coding agents work here, each Task in its own real terminal.",
        "When one needs you, Moru turns amber and waits.",
        "A few quick choices first. Each can change later in Settings.",
    )

    override val themeTitle = "Pick a look"
    override val themeBody = "More themes are in Settings › Appearance."
    override val appleStyleTitle = "Apple style"
    override val appleStyleBody = "Liquid Glass surfaces and rounded controls."
    override val doneTitle = "You're all set"
    override val doneBody = "Open a project folder to begin. Your choices are in Settings, and this welcome is there too."
    override val openProject = "Open a project…"
}
