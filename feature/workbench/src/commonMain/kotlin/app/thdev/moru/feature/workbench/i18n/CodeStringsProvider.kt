package app.thdev.moru.feature.workbench.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The Code tab table for [language]. Tests use this directly; screens use the composable overload. */
internal fun codeStrings(language: UiLanguage): CodeStrings =
    when (language) {
        UiLanguage.English -> EnglishCodeStrings
        UiLanguage.Korean -> KoreanCodeStrings
    }

/** The Code tab text in the language the app resolved. English when none was provided. */
@Composable
internal fun codeStrings(): CodeStrings = codeStrings(LocalUiLanguage.current)
