package app.thdev.moru.feature.workbench.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The Result pane table for [language]. Tests use this directly; screens use the composable overload. */
internal fun resultStrings(language: UiLanguage): ResultStrings =
    when (language) {
        UiLanguage.English -> EnglishResultStrings
        UiLanguage.Korean -> KoreanResultStrings
    }

/** The Result pane text in the language the app resolved. English when none was provided. */
@Composable
internal fun resultStrings(): ResultStrings = resultStrings(LocalUiLanguage.current)
