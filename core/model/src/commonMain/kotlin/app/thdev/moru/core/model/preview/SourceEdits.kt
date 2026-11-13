package app.thdev.moru.core.model.preview

/**
 * What changed in a Task's folder, read for the Live Web marks: each changed file's stamp (size and time, so a save
 * of an already changed file shows), keyed by its path from the repository root; which of them have changes not yet
 * approved ([unstaged]); the edits Moru Core hooks recorded lately; and the areas agents have claimed to work on.
 */
data class SourceEdits(
    val stamps: Map<String, String>,
    val agentEdits: List<AgentEdit> = emptyList(),
    val unstaged: Set<String> = emptySet(),
    val claims: List<WorkClaim> = emptyList(),
)
