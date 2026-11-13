package app.thdev.moru.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val CatalogSelectionRowId = "default"

/** Single-row persisted navigation selection for the global catalog. */
@Entity(tableName = "catalog_selection")
data class CatalogSelectionEntity(
    @PrimaryKey val id: String = CatalogSelectionRowId,
    @ColumnInfo(name = "selected_project_id") val selectedProjectId: String?,
    @ColumnInfo(name = "selected_task_id") val selectedTaskId: String?,
)
