package app.thdev.moru.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One terminal of a [WorkbenchLayout]. [hostKey] names it in the terminal host, where it may still be running;
 * [startupCommand] is what to run only when it is not, which is not necessarily what it first ran. A [lab] session is
 * an agent conversation with no terminal: [startupCommand] names its agent and [hostKey] the conversation Moru kept.
 */
@Serializable
data class SavedTerminal(
    val title: String? = null,
    @SerialName("web_preview") val webPreview: Boolean = false,
    val shell: String = "zsh",
    @SerialName("working_directory") val workingDirectory: String = ".",
    @SerialName("task_id") val taskId: String? = null,
    val url: String? = null,
    @SerialName("startup_command") val startupCommand: String? = null,
    @SerialName("host_key") val hostKey: String? = null,
    val lab: Boolean = false,
)
