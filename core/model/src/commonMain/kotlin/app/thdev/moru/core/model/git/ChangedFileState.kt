package app.thdev.moru.core.model.git

/**
 * One changed file as a quick status read sees it: a [stamp] (size and time) that changes on every save, and whether
 * its working copy differs from the index ([unstaged]; a file git does not track yet counts as unstaged).
 */
data class ChangedFileState(val stamp: String, val unstaged: Boolean)
