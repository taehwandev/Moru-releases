package app.thdev.moru.feature.git.changes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val REFRESH_MILLIS = 3_000L

/**
 * The uncommitted changes of [workspacePath], re-read every few seconds so the Result follows a running agent.
 * It is null until the first read; a re-read keeps the previous result instead of blanking it, and a different
 * folder starts from null. Changing [refreshKey] reads again at once.
 */
@Composable
fun rememberWorkingChanges(
    workspacePath: String,
    load: (workspacePath: String) -> CommitDiffResult,
    refreshKey: Int = 0,
): State<CommitDiffResult?> {
    val result = remember(workspacePath) { mutableStateOf<CommitDiffResult?>(null) }
    // Each read runs git over the whole folder, so it repeats only while Moru's window has focus; on return it reads at
    // once, keeping what it showed until then.
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(workspacePath, load, refreshKey, focused) {
        while (true) {
            result.value = withContext(Dispatchers.IO) { load(workspacePath) }
            if (!focused) break
            delay(REFRESH_MILLIS)
        }
    }
    return result
}
