package app.thdev.moru.feature.notifications

import app.thdev.moru.core.designsystem.theme.UiLanguage
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/** Today's notifications show only their time and older ones only their date, so the label stays short in a row. */
internal actual fun notificationTimestampLabel(epochMillis: Long, language: UiLanguage): String {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate() == LocalDate.now(zone)
    val format = if (today) DateFormat.getTimeInstance(DateFormat.SHORT, language.notificationLocale())
    else DateFormat.getDateInstance(DateFormat.MEDIUM, language.notificationLocale())
    return format.format(Date(epochMillis))
}

private fun UiLanguage.notificationLocale(): Locale = when (this) {
    UiLanguage.English -> Locale.ENGLISH
    UiLanguage.Korean -> Locale.KOREAN
}
