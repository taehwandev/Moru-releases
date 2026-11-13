package app.thdev.moru.core.ui.usage

import kotlin.test.Test
import kotlin.test.assertEquals

class TokenCountFormatTest {
    @Test
    fun smallCountsAreShownInFull() {
        assertEquals("0", formatTokenCount(0))
        assertEquals("999", formatTokenCount(999))
    }

    @Test
    fun thousandsKeepOneDecimalUnderTenAndNoTrailingZero() {
        assertEquals("1K", formatTokenCount(1_000))
        assertEquals("1.2K", formatTokenCount(1_234))
        assertEquals("9.9K", formatTokenCount(9_999))
        assertEquals("12K", formatTokenCount(12_345))
        assertEquals("999K", formatTokenCount(999_999))
    }

    @Test
    fun millionsAndBillionsUseTheirSuffix() {
        assertEquals("1M", formatTokenCount(1_000_000))
        assertEquals("1.2M", formatTokenCount(1_240_000))
        assertEquals("18M", formatTokenCount(18_700_000))
        assertEquals("1.5B", formatTokenCount(1_500_000_000))
    }
}
