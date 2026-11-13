package app.thdev.moru.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted Work Context for a Task. Stores bounded descriptors only — a
 * lifecycle label plus optional branch/worktree locators — never a claim that an
 * OS process is alive. One row per Task (task_id is the key).
 */
@Entity(tableName = "catalog_work_context")
data class WorkContextEntity(
    @PrimaryKey @ColumnInfo(name = "task_id") val taskId: String,
    val id: String,
    val lifecycle: String,
    val branch: String?,
    @ColumnInfo(name = "worktree_path") val worktreePath: String?,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
)
