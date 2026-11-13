package app.thdev.moru.feature.workbench


/** One Project row in the sidebar with plain-language availability and expansion. */
data class ProjectRow(
    val id: String,
    val displayName: String,
    val rootPath: String?,
    val availabilityLabel: String?,
    val isAvailable: Boolean,
    val expanded: Boolean,
    val selected: Boolean,
    val tasks: List<TaskRow>,
) {
    /** How many of this Project's Tasks need the user, so a collapsed Project still shows them. */
    val attentionCount: Int get() = tasks.count { task -> task.group == TaskGroup.NeedsAttention }
}
