package app.thdev.moru.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Features still being tried out; each is off until the user turns it on. */
@Serializable
data class ExperimentalSettings(
    /** New sessions can open as a Lab: Claude or Codex drawn as a conversation instead of a terminal. */
    @SerialName("lab_sessions")
    val labSessions: Boolean = false,
    /** Another Task's terminals can be opened beside the selected Task, and its Result picked in the Result pane. */
    @SerialName("tasks_beside")
    val tasksBeside: Boolean = false,
    /** An emulator or simulator can be put among a Task's terminals as a cell of its own. */
    @SerialName("devices_among_terminals")
    val devicesAmongTerminals: Boolean = false,
    /** A Project can open in a window of its own, beside the main window. */
    @SerialName("project_windows")
    val projectWindows: Boolean = false,
)
