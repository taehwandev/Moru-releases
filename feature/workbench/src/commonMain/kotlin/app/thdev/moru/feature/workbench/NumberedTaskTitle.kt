package app.thdev.moru.feature.workbench

/**
 * The title for a Task created without a description: the Project's name and the next number after the highest one
 * its Tasks already use ("Moru task 3"), so the title says where the Task belongs rather than which agent ran first.
 */
fun nextNumberedTaskTitle(projectName: String, existingTitles: List<String>): String {
    return app.thdev.moru.core.domain.catalog.nextNumberedTaskTitle(projectName, existingTitles)
}
