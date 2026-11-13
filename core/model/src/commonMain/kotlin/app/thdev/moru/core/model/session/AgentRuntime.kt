package app.thdev.moru.core.model.session

/** What a session runs. [Named] carries a program name that is shown as it is; the other two are described in the language tables. */
sealed interface AgentRuntime {
    data object Terminal : AgentRuntime
    data object WebPreview : AgentRuntime
    data class Named(val name: String) : AgentRuntime
}
