package app.thdev.moru.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where the user keeps their own agent workflows; nothing here is assumed to exist. */
@Serializable
data class AgentWorkflowSettings(
    // Overrides MORU_HOME and the ~/.moru/core and ~/git/moru-core guesses when set.
    @SerialName("moru_root")
    val moruRoot: String? = null,
    @SerialName("custom_roots")
    val customRoots: List<String> = emptyList(),
)
