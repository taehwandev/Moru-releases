package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.files.ProjectExplorerPolicy
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry

internal fun visibleProjectTreeRows(
    entries: List<WorkbenchProjectTreeEntry>,
    expanded: Set<String>,
    loaded: Map<String, List<WorkbenchProjectTreeEntry>>,
): List<WorkbenchProjectTreeEntry> = ProjectExplorerPolicy.visibleRows(entries, expanded, loaded)

internal fun deferredFoldersToLoad(
    rows: List<WorkbenchProjectTreeEntry>,
    expanded: Set<String>,
    loaded: Map<String, List<WorkbenchProjectTreeEntry>>,
): List<String> = ProjectExplorerPolicy.deferredFoldersToLoad(rows, expanded, loaded)
