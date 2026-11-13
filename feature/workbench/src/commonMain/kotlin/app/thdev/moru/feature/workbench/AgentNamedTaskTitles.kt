package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.TaskSummary

/**
 * New titles for a Project's Tasks still named after the agent they first ran ("Codex session"), as earlier versions
 * named a Task started without a description. Oldest first, each takes the next "<Project> task N" after the numbers
 * already in use; every other Task keeps its title. Empty once none are left, so applying it again changes nothing.
 */
fun agentNamedTaskTitles(projectName: String, tasks: List<TaskSummary>, agentNames: List<String>): Map<String, String> {
    val legacyTitles = agentNames.map { name -> "$name session" }.toSet()
    val titles = tasks.map { task -> task.title }.toMutableList()
    return tasks
        .filter { task -> task.title in legacyTitles }
        .sortedBy { task -> task.createdAtEpochMillis }
        .associate { task ->
            val title = nextNumberedTaskTitle(projectName, titles)
            titles += title
            task.id to title
        }
}
