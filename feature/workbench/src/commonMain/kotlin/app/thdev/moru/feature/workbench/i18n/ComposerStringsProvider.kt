package app.thdev.moru.feature.workbench.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The composer table for [language]. Tests use this directly; screens use the composable overload. */
internal fun composerStrings(language: UiLanguage): ComposerStrings =
    when (language) {
        UiLanguage.English -> EnglishComposerStrings
        UiLanguage.Korean -> KoreanComposerStrings
    }

/** The composer text in the language the app resolved. English when no language was provided. */
@Composable
internal fun composerStrings(): ComposerStrings = composerStrings(LocalUiLanguage.current)
