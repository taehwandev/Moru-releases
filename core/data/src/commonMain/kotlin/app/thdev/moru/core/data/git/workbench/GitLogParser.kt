package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.history.GitCommit

/** Compatibility entry point; portable Git interpretation lives in core:domain. */
const val GIT_LOG_PRETTY_FORMAT = app.thdev.moru.core.domain.git.GIT_LOG_PRETTY_FORMAT

fun parseGitLog(output: String): List<GitCommit> = app.thdev.moru.core.domain.git.parseGitLog(output)
