package app.thdev.moru.feature.git.i18n

internal object EnglishHistoryStrings : HistoryStrings {
    override val title = "History"
    override val refresh = "Refresh"
    override val loading = "Loading history…"
    override val noCommits = "No commits yet."
    override fun commitCount(count: Int, truncated: Boolean): String {
        val base = if (count == 1) "1 commit" else "$count commits"
        return if (truncated) "$base · recent history only" else base
    }
    override val diffNotConnected = "Viewing changes is not connected."
    override fun workingChanges(count: Int) = when (count) {
        0 -> "Working folder · no changes"
        1 -> "Working folder · 1 changed file"
        else -> "Working folder · $count changed files"
    }
    override val workingFolder = "Working folder"
    override val columnGraph = "Graph"
    override val columnDescription = "Description"
    override val columnAuthor = "Author"
    override val columnDate = "Date"
    override val columnCommit = "Commit"

    override val diffLoading = "Loading changes…"
    override val noFileChanges = "This commit changes no files."
    override fun authorLine(name: String, email: String, isMerge: Boolean, parentCount: Int): String {
        val kind = when {
            isMerge -> "merge commit"
            parentCount == 1 -> "1 parent"
            else -> "$parentCount parents"
        }
        return "$name <$email> · $kind"
    }

    override val changesTitle = "Changes"
    override val changesSubtitle = "Changes not committed yet. This updates by itself while an agent works."
    override val changesLoading = "Loading changes…"
    override val noChanges = "There are no uncommitted changes."

    override val resizeGitDetail = "Resize Git detail pane"
    override val resetPaneSize = "Reset pane size"
}
