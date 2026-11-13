package app.thdev.moru.core.ui.time

import app.thdev.moru.core.ui.i18n.CoreUiStrings

private const val MINUTE = 60L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR
private const val WEEK = 7 * DAY
private const val MONTH = 30 * DAY
private const val YEAR = 365 * DAY

/**
 * A short "how long ago" label in the language of [strings] for a commit time. [nowEpochSeconds] is passed in
 * so the result is deterministic. A time in the future (clock skew) reads as just now.
 */
fun formatRelativeTime(strings: CoreUiStrings, nowEpochSeconds: Long, thenEpochSeconds: Long): String {
    val elapsed = nowEpochSeconds - thenEpochSeconds
    return when {
        elapsed < MINUTE -> strings.agoJustNow
        elapsed < HOUR -> strings.agoMinutes(elapsed / MINUTE)
        elapsed < DAY -> strings.agoHours(elapsed / HOUR)
        elapsed < WEEK -> strings.agoDays(elapsed / DAY)
        elapsed < MONTH -> strings.agoWeeks(elapsed / WEEK)
        // 30-day months run 5 days short of a year, so cap at 11 instead of showing 12 months.
        elapsed < YEAR -> strings.agoMonths((elapsed / MONTH).coerceAtMost(11))
        else -> strings.agoYears(elapsed / YEAR)
    }
}
