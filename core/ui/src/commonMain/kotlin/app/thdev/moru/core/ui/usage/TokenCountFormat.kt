package app.thdev.moru.core.ui.usage

private const val THOUSAND = 1_000L
private const val MILLION = 1_000_000L
private const val BILLION = 1_000_000_000L

/** Short token counts for a narrow panel: 950, 1.2K, 34K, 5.6M, 1.2B. */
fun formatTokenCount(tokens: Long): String =
    when {
        tokens < THOUSAND -> tokens.toString()
        tokens < MILLION -> scaled(tokens, THOUSAND, "K")
        tokens < BILLION -> scaled(tokens, MILLION, "M")
        else -> scaled(tokens, BILLION, "B")
    }

/** One decimal below 10 units, none above, and never a trailing ".0". */
private fun scaled(tokens: Long, unit: Long, suffix: String): String {
    val tenths = tokens * 10 / unit
    return if (tenths >= 100 || tenths % 10 == 0L) {
        "${tenths / 10}$suffix"
    } else {
        "${tenths / 10}.${tenths % 10}$suffix"
    }
}
