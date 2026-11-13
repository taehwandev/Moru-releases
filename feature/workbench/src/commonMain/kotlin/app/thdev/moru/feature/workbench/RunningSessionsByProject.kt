package app.thdev.moru.feature.workbench

import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState

/** How many live terminal sessions belong to each Project's Tasks; a Project with none is absent. */
fun runningSessionsByProject(
    state: ProjectTaskSidebarState,
    snapshots: List<TerminalSessionSnapshot>,
): Map<String, Int> {
    val projectOfTask = state.projects.flatMap { project -> project.tasks.map { task -> task.id to project.id } }.toMap()
    return snapshots
        .filter { snapshot -> snapshot.status == TerminalSessionStatus.READY || snapshot.status == TerminalSessionStatus.STARTING }
        .mapNotNull { snapshot -> snapshot.spec.attachedItemId?.let(projectOfTask::get) }
        .groupingBy { projectId -> projectId }
        .eachCount()
}
