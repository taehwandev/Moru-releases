package app.thdev.moru.core.ui.i18n

/** Text of the shared widgets in core:ui: the diff views and relative times. */
interface CoreUiStrings {
    // Diff browser
    val binaryFile: String
    val renamedOnly: String
    val noLineChanges: String
    fun filesSummary(files: Int, additions: Int, deletions: Int, truncated: Boolean): String

    // Relative time
    val agoJustNow: String
    fun agoMinutes(count: Long): String
    fun agoHours(count: Long): String
    fun agoDays(count: Long): String
    fun agoWeeks(count: Long): String
    fun agoMonths(count: Long): String
    fun agoYears(count: Long): String
}
