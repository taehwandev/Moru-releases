package app.thdev.moru.app

import app.thdev.moru.core.model.TaskSortOrder
import app.thdev.moru.feature.settings.SettingsEditorState

/** Persist only valid permutations of the displayed siblings through the existing settings editor. */
internal class SidebarOrderChanges(
    private val navigation: WorkbenchNavigation,
    private val editor: SettingsEditorState,
) {
    fun projects(ids: List<String>) {
        val current = navigation.sidebar.projects.map { it.id }
        if (!isMove(current, ids)) return
        editor.change { it.copy(general = it.general.copy(projectOrder = ids.toList())) }
    }

    fun tasks(projectId: String, ids: List<String>) {
        val current = navigation.sidebar.projects.firstOrNull { it.id == projectId }?.tasks?.map { it.id } ?: return
        if (!isMove(current, ids)) return
        val displayed = navigation.sidebar.projects.associate { project -> project.id to project.tasks.map { it.id } }
        editor.change { settings -> settings.copy(general = settings.general.copy(
            taskSortOrder = TaskSortOrder.Manual,
            taskOrder = settings.general.taskOrder +
                (if (settings.general.taskSortOrder == TaskSortOrder.Manual) emptyMap() else displayed) +
                (projectId to ids.toList()),
        )) }
    }

    private fun isMove(current: List<String>, next: List<String>): Boolean =
        current != next && current.size == next.size && next.toSet().size == next.size && current.toSet() == next.toSet()
}
