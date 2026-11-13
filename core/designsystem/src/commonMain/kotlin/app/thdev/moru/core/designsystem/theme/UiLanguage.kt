package app.thdev.moru.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The language the screens draw their own text in, already resolved from the user's setting. Screens read
 * it through [LocalUiLanguage] and pick their string table; they never look at the computer's locale.
 */
enum class UiLanguage(
    /** The ISO 639-1 code content made for other languages (plugins, documents) is keyed by. */
    val code: String,
) {
    English("en"),
    Korean("ko"),
}

/** English unless the app provides otherwise, so a screen drawn without the app shell reads as English. */
val LocalUiLanguage = staticCompositionLocalOf { UiLanguage.English }
