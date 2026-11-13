package app.thdev.moru.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAtMillis DESC, rowid DESC LIMIT 500")
    abstract suspend fun load(): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insert(notifications: List<NotificationEntity>)

    @Query("DELETE FROM notifications WHERE id NOT IN (SELECT id FROM notifications ORDER BY createdAtMillis DESC, rowid DESC LIMIT 500)")
    protected abstract suspend fun trim()

    @Query("DELETE FROM notifications WHERE taskId = :taskId AND id != :keepId")
    protected abstract suspend fun deleteTaskRecordsExcept(taskId: String, keepId: String)

    /** Each record replaces its Task's earlier one, so the history keeps one record per Task; a repeated id is ignored. */
    @Transaction
    open suspend fun appendBounded(notifications: List<NotificationEntity>) {
        notifications.forEach { notification ->
            deleteTaskRecordsExcept(notification.taskId, notification.id)
            insert(listOf(notification))
        }
        trim()
    }

    @Query("UPDATE notifications SET `read` = 1 WHERE id = :id")
    abstract suspend fun markRead(id: String)

    @Query("UPDATE notifications SET `read` = 1 WHERE `read` = 0 AND taskId IN (:taskIds)")
    abstract suspend fun markTasksRead(taskIds: List<String>)

    @Query("UPDATE notifications SET `read` = 1 WHERE `read` = 0")
    abstract suspend fun markAllRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Query("DELETE FROM notifications")
    abstract suspend fun clear()
}
