package app.thdev.moru.core.model

import kotlinx.serialization.Serializable

/** A Task's Code tab in a [WorkbenchLayout]: the worktree each of its terminals showed, by the terminal's host key. */
@Serializable
data class SavedCodePanel(
    val worktrees: Map<String, String> = emptyMap(),
)
