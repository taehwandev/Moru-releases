package app.thdev.moru.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val TerminalWorkspaceStateRowId = "default"

@Entity(tableName = "terminal_workspaces")
data class TerminalWorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(tableName = "terminal_workspace_state")
data class TerminalWorkspaceStateEntity(
    @PrimaryKey val id: String = TerminalWorkspaceStateRowId,
    @ColumnInfo(name = "selected_workspace_id") val selectedWorkspaceId: String,
)
