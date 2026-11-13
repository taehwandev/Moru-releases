package app.thdev.moru.feature.workbench.beside

import app.thdev.moru.feature.workbench.ProjectRow

/**
 * The saved list behind Tasks beside. A window shows one Task beside the selected Task, and only one of the same
 * Project; the list keeps one entry per Project, so each Project remembers its own Task beside.
 */
object BesideTaskIds {
    /** The Task shown beside [selectedTaskId]: the first saved Task of the same Project other than it. */
    fun shown(saved: List<String>, selectedTaskId: String?, projects: List<ProjectRow>): String? {
        val siblings = projectOf(selectedTaskId, projects)?.tasks?.map { it.id }?.toSet() ?: return null
        return saved.firstOrNull { it != selectedTaskId && it in siblings }
    }

    /**
     * Closes [taskId] when it is saved beside, or opens it in place of its Project's Task beside. Entries no longer
     * allowed (a whole Project, from the replaced Project beside) are dropped on the way.
     */
    fun toggle(saved: List<String>, taskId: String, projects: List<ProjectRow>): List<String> {
        val kept = saved.filterNot { it.startsWith(RetiredProjectPrefix) }
        if (taskId in kept) return kept - taskId
        val siblings = projectOf(taskId, projects)?.tasks?.map { it.id }?.toSet().orEmpty()
        return kept.filterNot { it in siblings } + taskId
    }

    private fun projectOf(taskId: String?, projects: List<ProjectRow>): ProjectRow? =
        projects.firstOrNull { project -> project.tasks.any { it.id == taskId } }

    private const val RetiredProjectPrefix = "project:"
}
