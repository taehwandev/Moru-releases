package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.terminal.SessionResults
import app.thdev.moru.feature.codepanel.CodePanelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Counts, once per result, the uncommitted files in the checkout each session works in, the same checkout the
 * sidebar's Code tool shows, so the Moru view can say what changed next to what the agent reported.
 */
@Composable
internal fun ReportResultChanges(source: CodePanelSource) {
    LaunchedEffect(source) {
        val counted = mutableMapOf<String, String>()
        SessionResults.results.collect { results ->
            results.forEach { (sessionId, text) ->
                if (counted[sessionId] == text) return@forEach
                counted[sessionId] = text
                launch {
                    val count = withContext(Dispatchers.IO) { runCatching { uncommittedFiles(source, sessionId) }.getOrNull() }
                    if (SessionResults.results.value[sessionId] == text) SessionResults.recordChanges(sessionId, count)
                }
            }
        }
    }
}

/** The distinct uncommitted paths across the checkouts the session works in, or null when none can be read. */
internal fun uncommittedFiles(source: CodePanelSource, sessionId: String): Int? {
    val loaded = source.sessionWorktreeRoots(sessionId).map { root -> root to source.changes(root) }
        .filter { (_, changes) -> changes is CommitDiffResult.Loaded }
    if (loaded.isEmpty()) return null
    return loaded.flatMap { (root, changes) -> (changes as CommitDiffResult.Loaded).files.map { "$root/${it.displayPath}" } }.distinct().size
}
