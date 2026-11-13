package app.thdev.moru.feature.codepanel.i18n

internal object EnglishCodePanelStrings : CodePanelStrings {
    override val worktree = "Worktree"
    override fun mainWorktree(label: String) = "$label · project folder"
    override val loading = "Reading changes…"
    override val noChanges = "No changes in this worktree yet."
    override val notRepository = "This folder is not in a Git repository."
    override val codeMode = "Code"
    override val diffMode = "Diff"
    override fun unchangedLines(count: Int) = if (count == 1) "1 unchanged line" else "$count unchanged lines"
}
