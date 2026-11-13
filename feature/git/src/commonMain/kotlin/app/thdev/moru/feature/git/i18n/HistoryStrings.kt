package app.thdev.moru.feature.git.i18n

/** Text of the Git destination: the commit list, the commit diff pane, the diff file browser and the workspace. */
internal interface HistoryStrings {
    // History screen
    val title: String
    val refresh: String
    val loading: String
    val noCommits: String
    fun commitCount(count: Int, truncated: Boolean): String
    val diffNotConnected: String
    fun workingChanges(count: Int): String
    val workingFolder: String

    // Commit list column names
    val columnGraph: String
    val columnDescription: String
    val columnAuthor: String
    val columnDate: String
    val columnCommit: String

    // Commit diff pane
    val diffLoading: String
    val noFileChanges: String
    fun authorLine(name: String, email: String, isMerge: Boolean, parentCount: Int): String

    // Uncommitted changes screen
    val changesTitle: String
    val changesSubtitle: String
    val changesLoading: String
    val noChanges: String

    // Git workspace pane divider
    val resizeGitDetail: String
    val resetPaneSize: String
}
