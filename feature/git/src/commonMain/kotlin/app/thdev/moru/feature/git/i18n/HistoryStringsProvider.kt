package app.thdev.moru.feature.git.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The history screens' table for [language]. Tests use this directly; screens use the composable overload. */
internal fun historyStrings(language: UiLanguage): HistoryStrings =
    when (language) {
        UiLanguage.English -> EnglishHistoryStrings
        UiLanguage.Korean -> KoreanHistoryStrings
    }

/** The history screens' text in the language the app resolved. English when none was provided. */
@Composable
internal fun historyStrings(): HistoryStrings = historyStrings(LocalUiLanguage.current)
