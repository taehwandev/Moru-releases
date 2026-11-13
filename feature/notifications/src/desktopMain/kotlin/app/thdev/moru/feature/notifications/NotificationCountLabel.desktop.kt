package app.thdev.moru.feature.notifications

import app.thdev.moru.core.designsystem.theme.UiLanguage
import java.text.NumberFormat
import java.util.Locale

internal actual fun notificationCountLabel(count: Int, language: UiLanguage): String =
    NumberFormat.getIntegerInstance(language.notificationLocale()).format(count)

private fun UiLanguage.notificationLocale(): Locale = when (this) {
    UiLanguage.English -> Locale.ENGLISH
    UiLanguage.Korean -> Locale.KOREAN
}
