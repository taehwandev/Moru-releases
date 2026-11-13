package app.thdev.moru.feature.git

/** Discrete Git counts use the target's locale-aware integer formatter. */
internal expect fun formatGitCount(value: Int): String
