package app.thdev.moru.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class ProjectCatalogDao {
    @Query("SELECT * FROM catalog_project ORDER BY created_at ASC")
    abstract suspend fun listProjects(): List<ProjectEntity>

    @Query("SELECT * FROM catalog_project_root ORDER BY sort_order ASC")
    abstract suspend fun listRoots(): List<ProjectRootEntity>

    @Query("SELECT * FROM catalog_task ORDER BY updated_at DESC")
    abstract suspend fun listTasks(): List<TaskEntity>

    @Query("SELECT * FROM catalog_project_root WHERE canonical_path = :canonicalPath LIMIT 1")
    abstract suspend fun findRootByCanonicalPath(canonicalPath: String): ProjectRootEntity?

    @Query("SELECT * FROM catalog_project WHERE id = :projectId LIMIT 1")
    abstract suspend fun findProject(projectId: String): ProjectEntity?

    @Query("SELECT * FROM catalog_task WHERE id = :taskId LIMIT 1")
    abstract suspend fun findTask(taskId: String): TaskEntity?

    @Query("SELECT * FROM catalog_task_revision WHERE id = :revisionId LIMIT 1")
    abstract suspend fun findRevision(revisionId: String): TaskRevisionEntity?

    @Query("SELECT * FROM catalog_work_context WHERE task_id = :taskId LIMIT 1")
    abstract suspend fun findWorkContext(taskId: String): WorkContextEntity?

    @Query("SELECT * FROM catalog_work_context")
    abstract suspend fun listWorkContexts(): List<WorkContextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertWorkContext(workContext: WorkContextEntity)

    @Query("SELECT * FROM catalog_selection WHERE id = :id")
    abstract suspend fun getSelection(id: String = CatalogSelectionRowId): CatalogSelectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRoots(roots: List<ProjectRootEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRevision(revision: TaskRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSelection(selection: CatalogSelectionEntity)

    /** Register a Project and its roots atomically so a partial row can never appear. */
    @Transaction
    open suspend fun insertProjectWithRoots(
        project: ProjectEntity,
        roots: List<ProjectRootEntity>,
    ) {
        upsertProject(project)
        upsertRoots(roots)
    }

    /** Move a Project to its new folder: the root and the worktree locators that followed it change together. */
    @Transaction
    open suspend fun relocateProject(
        project: ProjectEntity,
        root: ProjectRootEntity,
        workContexts: List<WorkContextEntity>,
    ) {
        upsertProject(project)
        upsertRoots(listOf(root))
        workContexts.forEach { workContext -> upsertWorkContext(workContext) }
    }

    /** Create a Task and its first revision, and repoint the Project's last selected Task, atomically. */
    @Transaction
    open suspend fun insertTaskWithRevision(
        task: TaskEntity,
        revision: TaskRevisionEntity,
        project: ProjectEntity,
    ) {
        upsertTask(task)
        upsertRevision(revision)
        upsertProject(project)
    }

    /** Persist selection and the owning Project's last selected Task together so pointers can't diverge. */
    @Transaction
    open suspend fun applySelection(
        selection: CatalogSelectionEntity,
        project: ProjectEntity?,
    ) {
        upsertSelection(selection)
        if (project != null) {
            upsertProject(project)
        }
    }

    @Query("DELETE FROM catalog_task_revision WHERE task_id = :taskId")
    abstract suspend fun deleteRevisionsOf(taskId: String)

    @Query("DELETE FROM catalog_work_context WHERE task_id = :taskId")
    abstract suspend fun deleteWorkContextOf(taskId: String)

    @Query("DELETE FROM catalog_task WHERE id = :taskId")
    abstract suspend fun deleteTaskRow(taskId: String)

    @Query("DELETE FROM catalog_task_revision WHERE task_id IN (SELECT id FROM catalog_task WHERE project_id = :projectId)")
    abstract suspend fun deleteRevisionsOfProject(projectId: String)

    @Query("DELETE FROM catalog_work_context WHERE task_id IN (SELECT id FROM catalog_task WHERE project_id = :projectId)")
    abstract suspend fun deleteWorkContextsOfProject(projectId: String)

    @Query("DELETE FROM catalog_task WHERE project_id = :projectId")
    abstract suspend fun deleteTasksOfProject(projectId: String)

    @Query("DELETE FROM catalog_project_root WHERE project_id = :projectId")
    abstract suspend fun deleteRootsOfProject(projectId: String)

    @Query("DELETE FROM catalog_project WHERE id = :projectId")
    abstract suspend fun deleteProjectRow(projectId: String)

    /** Remove a Task and everything stored for it, and repoint the Project and selection that named it, atomically. */
    @Transaction
    open suspend fun deleteTaskCascade(
        taskId: String,
        project: ProjectEntity?,
        selection: CatalogSelectionEntity?,
    ) {
        deleteRevisionsOf(taskId)
        deleteWorkContextOf(taskId)
        deleteTaskRow(taskId)
        if (project != null) upsertProject(project)
        if (selection != null) upsertSelection(selection)
    }

    /** Remove a Project with its roots, Tasks, revisions and work contexts, and repoint the selection that named it, atomically. */
    @Transaction
    open suspend fun forgetProjectCascade(
        projectId: String,
        selection: CatalogSelectionEntity?,
    ) {
        deleteRevisionsOfProject(projectId)
        deleteWorkContextsOfProject(projectId)
        deleteTasksOfProject(projectId)
        deleteRootsOfProject(projectId)
        deleteProjectRow(projectId)
        if (selection != null) upsertSelection(selection)
    }

    /** Close a Project and, when it was the selected one, clear the selection in the same transaction. */
    @Transaction
    open suspend fun closeProjectAndClearSelection(
        project: ProjectEntity,
        selection: CatalogSelectionEntity?,
    ) {
        upsertProject(project)
        if (selection != null) {
            upsertSelection(selection)
        }
    }
}
