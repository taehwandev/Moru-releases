package app.thdev.moru.app

/** Display-only fallback; an embedded build version never grants installed-app update capability. */
internal fun appVersionLabel(installedVersion: String?): String =
    installedVersion?.takeIf(String::isNotBlank)?.let { "Moru $it" }
        ?: "Moru ${MoruBuildVersion.value} · Dev"
