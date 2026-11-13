package app.thdev.moru.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Global-catalog Project row. Stores ids and bounded metadata only. */
@Entity(tableName = "catalog_project")
data class ProjectEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "last_selected_task_id") val lastSelectedTaskId: String?,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    /** When the user closed the Project, or null while it is open. A closed Project keeps its Tasks but is not listed. */
    @ColumnInfo(name = "closed_at", defaultValue = "NULL") val closedAtEpochMillis: Long? = null,
)
