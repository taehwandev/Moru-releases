package app.thdev.moru.core.ui.i18n

import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The shared widgets' table for [language]. Tests use this directly; screens use the composable overload. */
fun coreUiStrings(language: UiLanguage): CoreUiStrings =
    when (language) {
        UiLanguage.English -> EnglishCoreUiStrings
        UiLanguage.Korean -> KoreanCoreUiStrings
    }
