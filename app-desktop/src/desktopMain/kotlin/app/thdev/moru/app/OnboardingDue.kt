package app.thdev.moru.app

import java.io.File

/**
 * Whether the welcome is due: no settings file yet (a new user), or one that has the welcome's key and so was written
 * by a Moru that offers it (left unfinished, the key says false). A file without the key is from before the welcome,
 * an existing user's, so it is not offered there.
 */
internal fun onboardingDue(settings: File): Boolean =
    !settings.exists() || runCatching { settings.readText() }.getOrDefault("").contains("\"onboarding_done\"")
