package app.thdev.moru.feature.settings.i18n

import app.thdev.moru.core.model.AppLanguage

/** Language names let readers find their own language whatever is selected. */
internal fun languageName(language: AppLanguage, system: String): String =
    when (language) {
        AppLanguage.System -> system
        AppLanguage.English -> "English"
        AppLanguage.Korean -> "한국어"
    }
