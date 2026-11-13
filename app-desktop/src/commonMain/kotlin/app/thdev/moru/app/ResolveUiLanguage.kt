package app.thdev.moru.app

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.model.AppLanguage

/**
 * The language to draw with. An explicit choice wins; [AppLanguage.System] follows the computer's language
 * tag (such as "ko" or "ko-KR") and falls back to English for any language the app has no text for.
 */
internal fun resolveUiLanguage(setting: AppLanguage, systemLanguageTag: String): UiLanguage =
    when (setting) {
        AppLanguage.English -> UiLanguage.English
        AppLanguage.Korean -> UiLanguage.Korean
        AppLanguage.System ->
            if (systemLanguageTag.substringBefore('-').substringBefore('_').equals("ko", ignoreCase = true)) {
                UiLanguage.Korean
            } else {
                UiLanguage.English
            }
    }
