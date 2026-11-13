package app.thdev.moru.feature.settings.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The Settings table for [language]. Tests use this directly; screens use the composable overload. */
internal fun settingsStrings(language: UiLanguage): SettingsStrings =
    when (language) {
        UiLanguage.English -> EnglishSettingsStrings
        UiLanguage.Korean -> KoreanSettingsStrings
    }

/** The Settings text in the language the app resolved. English when none was provided. */
@Composable
internal fun settingsStrings(): SettingsStrings = settingsStrings(LocalUiLanguage.current)
