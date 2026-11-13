package app.thdev.moru.feature.git.history

import app.thdev.moru.core.model.git.history.GitCommit

const val GIT_GRAPH_COLOR_COUNT = app.thdev.moru.core.domain.git.GIT_GRAPH_COLOR_COUNT

/** Compose keeps its API while sharing the exact lane algorithm with native hosts. */
fun layoutGitGraph(commits: List<GitCommit>, colorCount: Int = GIT_GRAPH_COLOR_COUNT): List<GitGraphRow> =
    app.thdev.moru.core.domain.git.layoutGitGraph(commits, colorCount)
