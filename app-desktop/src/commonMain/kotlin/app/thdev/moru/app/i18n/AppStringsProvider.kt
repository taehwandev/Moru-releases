package app.thdev.moru.app.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The table for [language]. Tests and non-composable code use this; screens use the composable overload. */
internal fun appStrings(language: UiLanguage): AppStrings =
    when (language) {
        UiLanguage.English -> EnglishAppStrings
        UiLanguage.Korean -> KoreanAppStrings
    }

/** The app shell text in the language the app resolved. English when no language was provided. */
@Composable
internal fun appStrings(): AppStrings = appStrings(LocalUiLanguage.current)
