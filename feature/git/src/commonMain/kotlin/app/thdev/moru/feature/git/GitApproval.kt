package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitAction

/** The action, folder, and revision stay frozen while the user reviews this proposal. */
internal data class GitApproval(
    val workspacePath: String,
    val repositoryPath: String,
    val revision: String,
    val branch: String,
    val action: GitAction,
    val title: String,
    val details: List<String>,
)
