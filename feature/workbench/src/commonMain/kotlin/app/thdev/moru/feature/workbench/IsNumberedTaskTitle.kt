package app.thdev.moru.feature.workbench

/** Whether [title] is still the number [nextNumberedTaskTitle] gave a Task of this Project, not a name of its own. */
fun isNumberedTaskTitle(projectName: String, title: String): Boolean = numberedTaskNumber(projectName, title) != null

internal fun numberedTaskNumber(projectName: String, title: String): Int? {
    val prefix = numberedTaskPrefix(projectName)
    return title.removePrefix(prefix).takeIf { title.startsWith(prefix) }?.toIntOrNull()
}

internal fun numberedTaskPrefix(projectName: String) = "$projectName task "
