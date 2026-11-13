package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.TaskSummary

/**
 * New titles for a Project's Tasks still numbered ("Moru task 3") whose first session now says what it is working
 * on: the summary its agent shows in the terminal title, or a Lab conversation's first request. A Task the user named,
 * or one already named this way, is no longer numbered and keeps its title, so a summary that later changes or
 * disappears renames nothing.
 */
fun sessionNamedTaskTitles(
    projectName: String,
    tasks: List<TaskSummary>,
    firstSessionSummaries: Map<String, String>,
): Map<String, String> = tasks
    .filter { task -> isNumberedTaskTitle(projectName, task.title) }
    .mapNotNull { task ->
        firstSessionSummaries[task.id]?.trim()?.take(MaxSessionTitleLength)?.takeIf { it.isNotEmpty() }
            ?.let { summary -> task.id to summary }
    }
    .toMap()

/** As long as a title taken from a description's first line. */
private const val MaxSessionTitleLength = 80
