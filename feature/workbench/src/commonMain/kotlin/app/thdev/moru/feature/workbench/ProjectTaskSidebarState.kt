package app.thdev.moru.feature.workbench

/**
 * Pure presentation state for the persistent Projects & Tasks sidebar. It holds
 * no runtime child state and starts no work, so building it is part of
 * metadata-only startup. See [projectTaskSidebarState] for the mapping.
 */
data class ProjectTaskSidebarState(
    val projects: List<ProjectRow>,
    val selectedProjectId: String?,
    val selectedTaskId: String?,
    val closedProjects: List<ClosedProjectRow> = emptyList(),
    /** The agent most recently used to start any Task, by name, or null. */
    val lastUsedAgent: String? = null,
    val autoCollapseInactiveSidebar: Boolean = false,
) {
    val isEmpty: Boolean get() = projects.isEmpty()

    /** Tasks across every Project that are waiting for the user. */
    val waitingCount: Int get() = projects.sumOf { project -> project.attentionCount }

    /** Tasks across every Project that are understanding, preparing, working or checking. */
    val runningCount: Int
        get() = projects.sumOf { project -> project.tasks.count { task -> task.group == TaskGroup.Running } }
}
