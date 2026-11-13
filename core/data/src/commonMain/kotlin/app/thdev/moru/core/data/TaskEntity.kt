package app.thdev.moru.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Global-catalog Task row. Navigation metadata only, not runtime child state. */
@Entity(
    tableName = "catalog_task",
    indices = [Index(value = ["project_id"])],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    val title: String,
    val status: String,
    @ColumnInfo(name = "current_revision_id") val currentRevisionId: String,
    @ColumnInfo(name = "current_result_id") val currentResultId: String?,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    /** The agent the Task was last started with, by name, or null when none was. Used to offer it again after a restart. */
    @ColumnInfo(name = "last_agent", defaultValue = "NULL") val lastAgent: String? = null,
)
