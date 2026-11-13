package app.thdev.moru.feature.git.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads one commit's diff off the UI thread. Null while loading, and again whenever [hash] changes. */
@Composable
internal fun rememberCommitDiff(hash: String, load: (String) -> CommitDiffResult): State<CommitDiffResult?> =
    produceState<CommitDiffResult?>(initialValue = null, hash, load) {
        value = null
        value = withContext(Dispatchers.Default) { load(hash) }
    }
