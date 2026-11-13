package app.thdev.moru.feature.codepanel.i18n

/** Text of the Code tab in a Task's Result pane. */
internal interface CodePanelStrings {
    val worktree: String
    fun mainWorktree(label: String): String
    val loading: String
    val noChanges: String
    val notRepository: String
    val codeMode: String
    val diffMode: String
    fun unchangedLines(count: Int): String
}
