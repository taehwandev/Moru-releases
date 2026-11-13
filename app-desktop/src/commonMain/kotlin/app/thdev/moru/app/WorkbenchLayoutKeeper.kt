package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.domain.WorkbenchLayoutRepository
import app.thdev.moru.core.model.WorkbenchLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The layout the user has left open, kept in memory for the screens to read and change, and written to [repository]
 * a moment after the last change rather than when the window closes, so a crash or a kill does not lose it. A burst
 * of changes is one write, made off the UI thread. [flush] writes a change still waiting, for the moment before quit.
 */
class WorkbenchLayoutKeeper(
    private val repository: WorkbenchLayoutRepository,
    initial: WorkbenchLayout = repository.loadLayout(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val saveDelayMillis: Long = 400L,
) {
    var layout by mutableStateOf(initial)
        private set

    private var pendingSave: Job? = null
    // [unsavedLock] is only held for a moment, so the UI thread never waits on a disk write; [writeLock] keeps writes in order.
    private val unsavedLock = Any()
    private val writeLock = Any()
    private var unsaved: WorkbenchLayout? = null

    fun update(transform: (WorkbenchLayout) -> WorkbenchLayout) {
        val next = transform(layout)
        if (next == layout) return
        layout = next
        synchronized(unsavedLock) { unsaved = next }
        pendingSave?.cancel()
        pendingSave = scope.launch {
            delay(saveDelayMillis)
            flush()
        }
    }

    /** Writes the last change if it has not been written yet; safe to call from any thread. */
    fun flush() {
        synchronized(writeLock) {
            val next = synchronized(unsavedLock) { unsaved } ?: return
            runCatching { repository.saveLayout(next) }
                .onSuccess { synchronized(unsavedLock) { if (unsaved === next) unsaved = null } }
                .onFailure { error -> System.err.println("Moru could not save the workbench layout: $error") }
        }
    }
}
