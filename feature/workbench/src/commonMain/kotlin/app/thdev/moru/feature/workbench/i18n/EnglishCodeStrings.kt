package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
internal object EnglishCodeStrings : CodeStrings {
    override val explorerTitle = "EXPLORER"
    override val newNote = "New note"
    override val closeNewNote = "Close the new note"
    override val quickOpenPlaceholder = "Quick open"
    override val noMatches = "No matches"
    override val newNotePlaceholder = "New note name"
    override val create = "Create"
    override val collapseAll = "Collapse all folders"
    override val expandAll = "Expand all folders"
    override val locateOpenFile = "Show the open file in the tree"
    override val explorerOptions = "Explorer options"
    override val compactRows = "Compact rows"
    override val followOpenFile = "Always show the open file"
    override val sortByName = "Sort by name"
    override val sortByType = "Sort by type"
    override val foldersFirst = "Folders first"
    override val showIgnored = "Show ignored files"
    override val mergeSingleFolders = "Merge single-folder paths"

    override val rename = "rename"
    override val confirmDelete = "confirm delete"
    override val cancel = "cancel"
    override val delete = "delete"
    override val close = "x"
    override val newPathPlaceholder = "New path/name"
    override val save = "Save"

    override val edit = "Edit"
    override val read = "Read"
    override val source = "Source"
    override val htmlPreview = "Preview"
    override val htmlRefresh = "Refresh"
    override val htmlStarting = "Opening HTML preview…"
    override val htmlChromeMissing = "Install Chrome to preview HTML here. You can still view and edit Source."
    override val htmlFailed = "HTML preview could not open. Try Refresh or switch to Source."
    override val unknownSize = "unknown size"
    override fun savedBytes(count: Long) = "Saved $count bytes"
    override val selectFilePrompt = "Pick a file on the left and its code opens here."
    override fun previewKind(status: WorkbenchFilePreviewStatus) = when (status) {
        WorkbenchFilePreviewStatus.TEXT -> "text"
        WorkbenchFilePreviewStatus.BINARY -> "binary"
        WorkbenchFilePreviewStatus.OVERSIZED -> "large"
        WorkbenchFilePreviewStatus.MISSING -> "missing"
        WorkbenchFilePreviewStatus.UNAVAILABLE -> "unavailable"
    }
    override fun previewMessage(status: WorkbenchFilePreviewStatus) = when (status) {
        WorkbenchFilePreviewStatus.TEXT -> "No preview content."
        WorkbenchFilePreviewStatus.BINARY -> "Binary files are not rendered inline."
        WorkbenchFilePreviewStatus.OVERSIZED -> "Large files show metadata only."
        WorkbenchFilePreviewStatus.MISSING -> "The file is no longer available."
        WorkbenchFilePreviewStatus.UNAVAILABLE -> "This file cannot be previewed."
    }

    override val gitTitle = "GIT"
    override val refresh = "refresh"
    override fun changedCount(count: Int) = "$count changed"
    override val clean = "clean"
    override fun gitSummary(staged: Int, unstaged: Int, untracked: Int) =
        "staged $staged / unstaged $unstaged / untracked $untracked"

}
