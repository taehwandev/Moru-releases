package app.thdev.moru.app

import app.thdev.moru.app.i18n.AppStrings

/** Shared by the native menu, first-run surface and title-bar Project menu. */
internal fun WorkbenchNavigation.openProjectFromPicker(pick: () -> String?, strings: AppStrings) {
    runCatching(pick)
        .onSuccess { it?.takeIf(String::isNotBlank)?.let(::openProject) }
        .onFailure { notice = strings.couldNotChooseFolder(it.message.orEmpty()) }
}

/** Asks where Project [projectId]'s folder is now, starting beside where it was, and points the Project there. */
internal fun WorkbenchNavigation.relocateProjectFromPicker(
    projectId: String,
    pick: (title: String, startIn: String?) -> String?,
    strings: AppStrings,
) {
    val besideOld = sidebar.projects.firstOrNull { it.id == projectId }?.rootPath?.substringBeforeLast('/')?.ifEmpty { null }
    runCatching { pick(strings.findProjectFolderTitle, besideOld) }
        .onSuccess { it?.takeIf(String::isNotBlank)?.let { path -> relocateProject(projectId, path) } }
        .onFailure { notice = strings.couldNotChooseFolder(it.message.orEmpty()) }
}
