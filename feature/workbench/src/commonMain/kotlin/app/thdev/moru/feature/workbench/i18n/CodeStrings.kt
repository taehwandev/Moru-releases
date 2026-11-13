package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
/** Text of the Code tab: the explorer, the open-file strip, the file preview/editor, the Git status box and the sessions toggle. */
internal interface CodeStrings {
    // Explorer
    val explorerTitle: String
    val newNote: String
    val closeNewNote: String
    val quickOpenPlaceholder: String
    val noMatches: String
    val newNotePlaceholder: String
    val create: String
    val collapseAll: String
    val expandAll: String
    val locateOpenFile: String
    val explorerOptions: String
    val compactRows: String
    val followOpenFile: String
    val sortByName: String
    val sortByType: String
    val foldersFirst: String
    val showIgnored: String
    val mergeSingleFolders: String

    // Open file strip
    val rename: String
    val confirmDelete: String
    val cancel: String
    val delete: String
    val close: String
    val newPathPlaceholder: String
    val save: String

    // Preview and editor
    val edit: String
    val read: String
    val source: String
    val htmlPreview: String
    val htmlRefresh: String
    val htmlStarting: String
    val htmlChromeMissing: String
    val htmlFailed: String
    val unknownSize: String
    fun savedBytes(count: Long): String
    val selectFilePrompt: String
    fun previewKind(status: WorkbenchFilePreviewStatus): String
    fun previewMessage(status: WorkbenchFilePreviewStatus): String

    // Git status box
    val gitTitle: String
    val refresh: String
    fun changedCount(count: Int): String
    val clean: String
    fun gitSummary(staged: Int, unstaged: Int, untracked: Int): String

}
