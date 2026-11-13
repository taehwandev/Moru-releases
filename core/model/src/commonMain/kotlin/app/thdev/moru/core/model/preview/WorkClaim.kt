package app.thdev.moru.core.model.preview

/**
 * An area an agent claimed before editing: [path] from the repository root, ending in "/" for a folder, held by
 * [holder] (a terminal's host key, or "pid:N" when the agent's shell did not pass one on).
 */
data class WorkClaim(val path: String, val holder: String)
