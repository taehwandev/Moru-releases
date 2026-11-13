package app.thdev.moru.core.model.preview

/** One edit an agent's hook recorded: the terminal it ran in ([session], its host key) and the files it wrote. */
data class AgentEdit(
    val session: String,
    val files: List<String>,
    val time: String,
)
