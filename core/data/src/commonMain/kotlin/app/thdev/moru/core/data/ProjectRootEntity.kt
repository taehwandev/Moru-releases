package app.thdev.moru.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Global-catalog Project root row. Stores safe path locators, not file contents. */
@Entity(
    tableName = "catalog_project_root",
    indices = [
        Index(value = ["project_id"]),
        Index(value = ["canonical_path"]),
    ],
)
data class ProjectRootEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    val role: String,
    @ColumnInfo(name = "last_known_path") val lastKnownPath: String,
    @ColumnInfo(name = "canonical_path") val canonicalPath: String?,
    val availability: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)
