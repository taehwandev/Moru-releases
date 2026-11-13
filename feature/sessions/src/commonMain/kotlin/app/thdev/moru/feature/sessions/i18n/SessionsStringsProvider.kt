package app.thdev.moru.feature.sessions.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The Sessions panel table for [language]. Tests use this directly; screens use the composable overload. */
internal fun sessionsStrings(language: UiLanguage): SessionsStrings =
    when (language) {
        UiLanguage.English -> EnglishSessionsStrings
        UiLanguage.Korean -> KoreanSessionsStrings
    }

/** The Sessions panel text in the language the app resolved. English when none was provided. */
@Composable
internal fun sessionsStrings(): SessionsStrings = sessionsStrings(LocalUiLanguage.current)
