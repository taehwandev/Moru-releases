package app.thdev.moru.feature.workbench.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.UiLanguage

/** The table for [language]. Tests use this directly; screens use the composable overload. */
internal fun workbenchStrings(language: UiLanguage): WorkbenchStrings =
    when (language) {
        UiLanguage.English -> EnglishWorkbenchStrings
        UiLanguage.Korean -> KoreanWorkbenchStrings
    }

/** The workbench text in the language the app resolved. English when the app shell did not provide one. */
@Composable
internal fun workbenchStrings(): WorkbenchStrings = workbenchStrings(LocalUiLanguage.current)
