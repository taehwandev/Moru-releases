package app.thdev.moru.core.ui.time

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.ui.i18n.coreUiStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {
    private val now = 1_000_000_000L

    private val korean = coreUiStrings(UiLanguage.Korean)
    private val english = coreUiStrings(UiLanguage.English)

    private fun ago(seconds: Long) = formatRelativeTime(korean, now, now - seconds)

    @Test
    fun underAMinuteAndFutureTimesReadAsJustNow() {
        assertEquals("방금", ago(0))
        assertEquals("방금", ago(59))
        assertEquals("방금", formatRelativeTime(korean, now, now + 500), "clock skew must not produce negative text")
    }

    @Test
    fun unitsSwitchExactlyAtTheirBoundaries() {
        assertEquals("1분 전", ago(60))
        assertEquals("59분 전", ago(3_599))
        assertEquals("1시간 전", ago(3_600))
        assertEquals("23시간 전", ago(86_399))
        assertEquals("1일 전", ago(86_400))
        assertEquals("6일 전", ago(6 * 86_400 + 5))
        assertEquals("1주 전", ago(7 * 86_400))
        assertEquals("4주 전", ago(29 * 86_400))
        assertEquals("1개월 전", ago(30 * 86_400))
        assertEquals("11개월 전", ago(364 * 86_400))
        assertEquals("1년 전", ago(365 * 86_400))
        assertEquals("3년 전", ago(3 * 365 * 86_400 + 1))
    }

    @Test
    fun englishUsesShortUnitsAndTheSameBoundaries() {
        fun enAgo(seconds: Long) = formatRelativeTime(english, now, now - seconds)
        assertEquals("just now", enAgo(59))
        assertEquals("1m ago", enAgo(60))
        assertEquals("23h ago", enAgo(86_399))
        assertEquals("1w ago", enAgo(7 * 86_400))
        assertEquals("11mo ago", enAgo(364 * 86_400))
        assertEquals("3y ago", enAgo(3 * 365 * 86_400 + 1))
    }
}
