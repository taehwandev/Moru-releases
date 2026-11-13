package app.thdev.moru.feature.workbench.beside

import app.thdev.moru.feature.workbench.TaskDisplayState

/**
 * A Task whose terminals are open beside the selected Task: its id, its title, where new terminals start, and the
 * state the sidebar shows for it.
 */
data class BesideTask(
    val id: String,
    val title: String,
    val folder: String,
    val state: TaskDisplayState = TaskDisplayState.Unknown,
)
