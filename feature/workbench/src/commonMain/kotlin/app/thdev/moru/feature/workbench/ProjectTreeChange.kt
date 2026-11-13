package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.files.ProjectExplorerPolicy
import app.thdev.moru.core.model.git.WorkbenchGitStatus

internal typealias ProjectTreeChange = ProjectExplorerPolicy.Change

internal fun projectTreeChanges(status: WorkbenchGitStatus): Map<String, ProjectTreeChange> =
    ProjectExplorerPolicy.changes(status.changes)
