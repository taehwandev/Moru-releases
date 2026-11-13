package app.thdev.moru.core.terminal

/** Ephemeral identity for one live permission request; history cannot reconstruct it. */
data class SessionPermissionRequest(
    val id: String,
    val question: String,
    val details: String,
    val choices: List<SessionPermissionChoice>,
)
