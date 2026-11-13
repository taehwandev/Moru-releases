package app.thdev.moru.core.domain.catalog

/** The next Project-relative title, shared by native and Compose callers. */
fun nextNumberedTaskTitle(projectName: String, existingTitles: List<String>): String {
    val highest = existingTitles.mapNotNull { numberedTaskNumber(projectName, it) }.maxOrNull() ?: 0
    return "$projectName task ${highest + 1}"
}

internal fun numberedTaskNumber(projectName: String, title: String): Int? {
    val prefix = "$projectName task "
    return title.removePrefix(prefix).takeIf { title.startsWith(prefix) }?.toIntOrNull()
}
