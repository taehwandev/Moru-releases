package app.thdev.moru.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class TerminalWorkspaceDao {
    @Query("SELECT * FROM terminal_workspaces ORDER BY sort_order ASC")
    abstract suspend fun listWorkspaces(): List<TerminalWorkspaceEntity>

    @Query("SELECT * FROM terminal_workspace_state WHERE id = :id")
    abstract suspend fun getWorkspaceState(
        id: String = TerminalWorkspaceStateRowId,
    ): TerminalWorkspaceStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertWorkspaces(workspaces: List<TerminalWorkspaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertWorkspaceState(state: TerminalWorkspaceStateEntity)

    @Query("DELETE FROM terminal_workspaces WHERE id NOT IN (:workspaceIds)")
    abstract suspend fun deleteWorkspacesNotIn(workspaceIds: List<String>)

    @Transaction
    open suspend fun replaceWorkspaceState(
        workspaces: List<TerminalWorkspaceEntity>,
        selectedWorkspaceId: String,
    ) {
        upsertWorkspaces(workspaces)
        deleteWorkspacesNotIn(workspaces.map { workspace -> workspace.id })
        upsertWorkspaceState(TerminalWorkspaceStateEntity(selectedWorkspaceId = selectedWorkspaceId))
    }
}
