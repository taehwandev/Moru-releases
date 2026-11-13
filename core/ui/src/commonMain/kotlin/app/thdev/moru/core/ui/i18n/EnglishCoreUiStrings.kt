package app.thdev.moru.core.ui.i18n

internal object EnglishCoreUiStrings : CoreUiStrings {
    override val binaryFile = "Binary file; its changes are not shown."
    override val renamedOnly = "The content is unchanged; only the name changed."
    override val noLineChanges = "No line changes to show. Only properties such as permissions may have changed."
    override fun filesSummary(files: Int, additions: Int, deletions: Int, truncated: Boolean): String {
        val base = (if (files == 1) "1 file" else "$files files") + " · +$additions −$deletions"
        return if (truncated) "$base · large change, showing part of it" else base
    }

    override val agoJustNow = "just now"
    override fun agoMinutes(count: Long) = "${count}m ago"
    override fun agoHours(count: Long) = "${count}h ago"
    override fun agoDays(count: Long) = "${count}d ago"
    override fun agoWeeks(count: Long) = "${count}w ago"
    override fun agoMonths(count: Long) = "${count}mo ago"
    override fun agoYears(count: Long) = "${count}y ago"
}
