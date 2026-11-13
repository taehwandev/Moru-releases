package app.thdev.moru.feature.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The branch each of [sessionIds] works on, by session id, as [reader] names it off the UI thread. It is read once,
 * then again every few seconds only while the window has focus, so an unwatched Moru runs no git or lsof. Sessions
 * [reader] names nothing for are left out; no [reader] shows no branches.
 */
@Composable
internal fun rememberSessionBranches(
    sessionIds: List<String>,
    folder: String,
    reader: ((sessionId: String, folder: String) -> String?)?,
): Map<String, String> {
    var branches by remember(folder) { mutableStateOf(emptyMap<String, String>()) }
    val focused = LocalWindowInfo.current.isWindowFocused
    val latestReader by rememberUpdatedState(reader)
    LaunchedEffect(sessionIds, folder, focused, reader == null) {
        while (true) {
            val read = latestReader
            branches = if (read == null) emptyMap() else withContext(Dispatchers.IO) {
                sessionIds.mapNotNull { id -> runCatching { read(id, folder) }.getOrNull()?.let { id to it } }.toMap()
            }
            if (read == null || !focused) break
            delay(BranchReadIntervalMillis)
        }
    }
    return branches
}

private const val BranchReadIntervalMillis = 3_000L
