package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.domain.NotificationRepository
import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.feature.notifications.NotificationInboxUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes receipt/read/delete writes and keeps failed writes available for retry. */
internal class NotificationInboxController(
    private val repository: NotificationRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(NotificationInboxUiState())
        private set
    private val mutex = Mutex()
    private val pending = linkedMapOf<String, TaskNotification>()
    private var failedAction: (suspend () -> Unit)? = null

    init { retry() }

    fun record(records: List<TaskNotification>) {
        records.forEach { pending[it.id] = it }
        while (pending.size > 500) pending.remove(pending.keys.first())
        perform { flushReceipts() }
    }

    fun markRead(id: String) = perform { repository.markRead(id) }

    /** Tasks with at least one unread record; the app icon and the inbox both count these. */
    val unreadTaskIds: Set<String>
        get() = state.notifications.filterNot { it.read }.mapTo(mutableSetOf()) { it.taskId }

    /** Marks the Tasks' records read; skips the write when none of them has an unread record. */
    fun markTasksRead(taskIds: Set<String>) {
        val unread = taskIds.filterTo(mutableSetOf()) { it in unreadTaskIds || pending.values.any { p -> p.taskId == it && !p.read } }
        // Pending receipts are written first so a receipt still on its way is marked read too.
        if (unread.isNotEmpty()) perform { flushReceipts(); repository.markTasksRead(unread) }
    }
    fun markAllRead() = perform { repository.markAllRead() }
    fun delete(id: String) = perform {
        repository.delete(id)
        pending.remove(id)
    }
    fun clear() {
        val pendingIds = pending.keys.toSet()
        perform {
            repository.clear()
            pendingIds.forEach { pending.remove(it) }
        }
    }

    fun retry() = perform(failedAction ?: { flushReceipts() })

    private suspend fun flushReceipts() {
        val batch = pending.values.toList()
        if (batch.isEmpty()) return
        repository.append(batch)
        batch.forEach { pending.remove(it.id) }
    }

    private fun perform(action: suspend () -> Unit) {
        scope.launch {
            mutex.withLock {
                state = state.copy(busy = true)
                var committed = false
                try {
                    action()
                    committed = true
                    flushReceipts()
                    val records = repository.load()
                    failedAction = null
                    state = state.copy(loading = false, notifications = records, error = null)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failedAction = action.takeUnless { committed }
                    state = state.copy(loading = false, error = "Could not update local notification history. Try again.")
                } finally {
                    state = state.copy(busy = false)
                }
            }
        }
    }
}
