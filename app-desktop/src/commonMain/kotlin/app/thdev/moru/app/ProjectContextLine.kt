package app.thdev.moru.app

import app.thdev.moru.app.i18n.AppStrings
import app.thdev.moru.app.i18n.EnglishAppStrings

/** The title the header shows: the project, and the selected Task after it. */
internal fun contextTitle(projectName: String?, taskTitle: String?, strings: AppStrings = EnglishAppStrings): String = when {
    projectName == null -> strings.noProjectSelected
    taskTitle == null -> strings.readyForTask(projectName)
    else -> "$projectName › $taskTitle"
}

internal fun contextDetail(
    branch: String?,
    folder: String,
    strings: AppStrings = EnglishAppStrings,
): String {
    val place = shortFolder(folder)
    return if (branch != null) strings.branchDetail(branch, place) else strings.folderDetail(place)
}

internal fun shortFolder(folder: String): String {
    val parts = folder.trimEnd('/').split('/').filter { part -> part.isNotEmpty() }
    return if (parts.size <= 2) folder else "…/" + parts.takeLast(2).joinToString("/")
}
