package app.thdev.moru.core.ui.i18n

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage

/** The shared widgets' text in the language the app resolved. English when none was provided. */
@Composable
fun coreUiStrings(): CoreUiStrings = coreUiStrings(LocalUiLanguage.current)
