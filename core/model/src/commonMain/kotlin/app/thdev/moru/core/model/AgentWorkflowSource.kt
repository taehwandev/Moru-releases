package app.thdev.moru.core.model

/** One workflow root found on this machine, or the configured one that is missing. */
data class AgentWorkflowSource(
    val kind: AgentWorkflowKind,
    val path: String,
    val found: Boolean,
    val skillCount: Int = 0,
    /** Instruction files at the root, such as AGENTS.md or CLAUDE.md. */
    val instructionFiles: List<String> = emptyList(),
    /** The entry in settings this came from, so the user can remove it; null when Moru found it itself. */
    val configuredRoot: String? = null,
)
