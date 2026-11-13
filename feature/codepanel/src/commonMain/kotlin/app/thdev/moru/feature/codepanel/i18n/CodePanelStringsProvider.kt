package app.thdev.moru.feature.codepanel.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The Code panel table for [language]. Tests use this directly; screens use the composable overload. */
internal fun codePanelStrings(language: UiLanguage): CodePanelStrings =
    when (language) {
        UiLanguage.English -> EnglishCodePanelStrings
        UiLanguage.Korean -> KoreanCodePanelStrings
    }

/** The Code panel text in the language the app resolved. English when none was provided. */
@Composable
internal fun codePanelStrings(): CodePanelStrings = codePanelStrings(LocalUiLanguage.current)
