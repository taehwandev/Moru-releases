package app.thdev.moru.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Global-catalog Task revision row bound to one execution-affecting outcome version. */
@Entity(
    tableName = "catalog_task_revision",
    indices = [Index(value = ["task_id"])],
)
data class TaskRevisionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    val ordinal: Int,
    @ColumnInfo(name = "desired_outcome") val desiredOutcome: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
)
