package app.thdev.moru.feature.codepanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.domain.code.CodeChangeWatchPolicy
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * What the Code panel of one Task shows, read from [source] off the UI thread. The reads are suspending loops the
 * panel runs in its own effects, so leaving the panel or changing what it follows cancels them.
 */
internal class TaskCodePanelStateHolder(
    private val source: CodePanelSource,
    private val state: CodePanelState,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val readIntervalMillis: Long = ReadIntervalMillis,
) {
    var worktrees by mutableStateOf(emptyList<CodeWorktree>())
        private set
    var checked by mutableStateOf(false)
        private set
    var changes by mutableStateOf<CommitDiffResult?>(null)
        private set

    // The root and stamp last read, so watching again after a pause rereads the changes only if they moved.
    private var watchedRoot: String? = null
    private var watchedStamp: String? = null

    /**
     * Every interval, where the terminal's programs work and which checkouts exist. With [repeat] false it reads once
     * and returns, as while Moru's window is in the background; otherwise it never returns.
     */
    suspend fun followWorktrees(taskKey: String, terminalKey: String, sessionId: String?, projectFolder: String, repeat: Boolean = true) {
        while (true) {
            val (found, roots) = withContext(io) {
                source.worktrees(projectFolder) to sessionId?.let(source::sessionWorktreeRoots).orEmpty()
            }
            worktrees = found
            checked = true
            state.observe(taskKey, terminalKey, roots, found)
            if (!repeat) return
            delay(readIntervalMillis)
        }
    }

    /**
     * Every interval, whether [root]'s changes moved, rereading them only when they did; never returns while there is
     * a [root], unless [repeat] is false, when it checks once. A different or no root starts from no changes; the same
     * root, watched again after a pause, keeps what it showed.
     */
    suspend fun watchChanges(root: String?, repeat: Boolean = true) {
        if (root != watchedRoot) {
            changes = null
            watchedRoot = root
            watchedStamp = null
        }
        if (root == null) return
        while (true) {
            val next = withContext(io) { source.changeStamp(root) }
            // A stamp that cannot be read says nothing moved; rereading every time would cost a git run per file.
            if (CodeChangeWatchPolicy.shouldReadChanges(hasSnapshot = changes != null, previousStamp = watchedStamp, nextStamp = next)) {
                changes = withContext(io) { source.changes(root) }
                watchedStamp = next
            }
            if (!repeat) return
            delay(readIntervalMillis)
        }
    }

    fun pick(taskKey: String, terminalKey: String, root: String) = state.pick(taskKey, terminalKey, root)

    fun shown(taskKey: String, terminalKey: String): String? = state.shown(taskKey, terminalKey)

    private companion object {
        const val ReadIntervalMillis = 2_000L
    }
}
