package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.files.ProjectExplorerPolicy
import app.thdev.moru.core.model.CodeExplorerOptions
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry

/** Compatibility entrypoint for shared preorder sorting, filtering and folder-chain merging. */
internal fun arrangeProjectTree(
    entries: List<WorkbenchProjectTreeEntry>,
    options: CodeExplorerOptions,
): List<WorkbenchProjectTreeEntry> = ProjectExplorerPolicy.arrange(entries, options)
