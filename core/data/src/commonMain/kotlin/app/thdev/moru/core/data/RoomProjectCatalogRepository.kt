package app.thdev.moru.core.data

import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.domain.ProjectCatalogSnapshot
import app.thdev.moru.core.domain.ProjectEntry
import app.thdev.moru.core.domain.ProjectRegistration
import app.thdev.moru.core.domain.ProjectRelocation
import app.thdev.moru.core.domain.TaskDetail
import app.thdev.moru.core.model.ProjectAvailability
import app.thdev.moru.core.model.ProjectRootRole
import app.thdev.moru.core.model.WorkContextDescriptor
import app.thdev.moru.core.model.Task
import app.thdev.moru.core.model.TaskStatus
import app.thdev.moru.core.model.wireValue
import app.thdev.moru.core.model.workContextPersistedLifecycleFromWireValue
import kotlinx.coroutines.runBlocking

/**
 * Room-backed [ProjectCatalogRepository]. Owns navigation metadata only and never
 * creates runtime children. Ids and timestamps are injected so the store is
 * deterministic under test. Entity/model mapping lives in [ProjectCatalogMappers].
 */
class RoomProjectCatalogRepository(
    private val database: MoruCatalogDatabase,
    private val canonicalizer: ProjectRootCanonicalizer,
    private val idGenerator: () -> String,
    private val nowEpochMillis: () -> Long,
) : ProjectCatalogRepository {

    override fun loadCatalog(): ProjectCatalogSnapshot = runBlocking {
        val dao = database.projectCatalogDao()
        // Availability is read from disk on every load, so a folder renamed or moved outside Moru shows as missing.
        val rootsByProject = dao.listRoots()
            .map { root -> root.copy(availability = canonicalizer.canonicalize(root.lastKnownPath).availability.wireValue) }
            .groupBy { root -> root.projectId }
        val tasksByProject = dao.listTasks().groupBy { task -> task.projectId }
        val lifecycleByTask = dao.listWorkContexts()
            .associate { workContext -> workContext.taskId to workContextPersistedLifecycleFromWireValue(workContext.lifecycle) }
        val selection = dao.getSelection()

        val entries = dao.listProjects().filter { projectEntity -> projectEntity.closedAtEpochMillis == null }.map { projectEntity ->
            ProjectEntry(
                project = ProjectCatalogMappers.projectFrom(projectEntity, rootsByProject[projectEntity.id].orEmpty()),
                tasks = tasksByProject[projectEntity.id].orEmpty()
                    .map { task -> ProjectCatalogMappers.summaryFrom(task, lifecycleByTask[task.id]) },
            )
        }

        val validProjectIds = entries.map { entry -> entry.project.id }.toSet()
        val validTaskIds = entries.flatMap { entry -> entry.tasks.map { task -> task.id } }.toSet()

        val closed = dao.listProjects()
            .filter { projectEntity -> projectEntity.closedAtEpochMillis != null }
            .sortedByDescending { projectEntity -> projectEntity.closedAtEpochMillis }
            .take(MaxClosedProjects)
            .map { projectEntity -> ProjectCatalogMappers.projectFrom(projectEntity, rootsByProject[projectEntity.id].orEmpty()) }

        ProjectCatalogSnapshot(
            projects = entries,
            selectedProjectId = selection?.selectedProjectId?.takeIf { id -> id in validProjectIds },
            selectedTaskId = selection?.selectedTaskId?.takeIf { id -> id in validTaskIds },
            closedProjects = closed,
        )
    }

    override fun registerProject(displayName: String, rootPath: String): ProjectRegistration = runBlocking {
        val dao = database.projectCatalogDao()
        val canonical = canonicalizer.canonicalize(rootPath)

        val existing = canonical.canonicalPath?.let { path -> dao.findRootByCanonicalPath(path) }
        if (existing != null) {
            val found = dao.findProject(existing.projectId)
            if (found != null) {
                // Opening the folder of a closed Project brings it back with its Tasks.
                val projectEntity = if (found.closedAtEpochMillis != null) {
                    found.copy(closedAtEpochMillis = null, updatedAtEpochMillis = nowEpochMillis()).also { dao.upsertProject(it) }
                } else {
                    found
                }
                val roots = dao.listRoots().filter { root -> root.projectId == projectEntity.id }
                return@runBlocking ProjectRegistration(
                    project = ProjectCatalogMappers.projectFrom(projectEntity, roots),
                    alreadyRegistered = true,
                )
            }
        }

        val now = nowEpochMillis()
        val resolvedName = displayName.trim()
            .ifEmpty { canonical.lastKnownPath.substringAfterLast('/').ifEmpty { "Project" } }
        val projectId = idGenerator()
        val projectEntity = ProjectEntity(
            id = projectId,
            displayName = resolvedName,
            lastSelectedTaskId = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        val rootEntity = ProjectRootEntity(
            id = idGenerator(),
            projectId = projectId,
            role = ProjectRootRole.Primary.wireValue,
            lastKnownPath = canonical.lastKnownPath,
            canonicalPath = canonical.canonicalPath,
            availability = canonical.availability.wireValue,
            sortOrder = 0,
        )
        dao.insertProjectWithRoots(projectEntity, listOf(rootEntity))

        ProjectRegistration(
            project = ProjectCatalogMappers.projectFrom(projectEntity, listOf(rootEntity)),
            alreadyRegistered = false,
        )
    }

    override fun closeProject(projectId: String) = runBlocking {
        val dao = database.projectCatalogDao()
        val projectEntity = dao.findProject(projectId) ?: return@runBlocking
        val now = nowEpochMillis()
        val selection = dao.getSelection()
        // Closing the open Project must not leave the app pointing at something it no longer lists.
        val clearSelection = selection?.selectedProjectId == projectId
        dao.closeProjectAndClearSelection(
            project = projectEntity.copy(closedAtEpochMillis = now, updatedAtEpochMillis = now),
            selection = if (clearSelection) selection?.copy(selectedProjectId = null, selectedTaskId = null) else null,
        )
    }

    override fun forgetClosedProject(projectId: String) = runBlocking {
        val dao = database.projectCatalogDao()
        val projectEntity = dao.findProject(projectId) ?: return@runBlocking
        // An open Project is still listed and in use; only the archive can be emptied.
        if (projectEntity.closedAtEpochMillis == null) return@runBlocking
        val selection = dao.getSelection()
        val selectedTaskBelongs = selection?.selectedTaskId?.let { taskId -> dao.findTask(taskId)?.projectId == projectId } == true
        val clearSelection = selection?.selectedProjectId == projectId || selectedTaskBelongs
        dao.forgetProjectCascade(
            projectId = projectId,
            selection = if (clearSelection) selection?.copy(selectedProjectId = null, selectedTaskId = null) else null,
        )
    }

    override fun renameProject(projectId: String, displayName: String) = runBlocking {
        val dao = database.projectCatalogDao()
        val name = displayName.replace(LineBreaks, " ").trim().take(MaxProjectNameLength)
        if (name.isEmpty()) return@runBlocking
        val projectEntity = dao.findProject(projectId) ?: return@runBlocking
        if (projectEntity.displayName == name) return@runBlocking
        dao.upsertProject(projectEntity.copy(displayName = name, updatedAtEpochMillis = nowEpochMillis()))
    }

    override fun relocateProject(projectId: String, rootPath: String): ProjectRelocation = runBlocking {
        val dao = database.projectCatalogDao()
        val projectEntity = dao.findProject(projectId) ?: return@runBlocking ProjectRelocation.NotFound
        val canonical = canonicalizer.canonicalize(rootPath)
        if (canonical.availability != ProjectAvailability.Available) return@runBlocking ProjectRelocation.FolderUnavailable
        val owner = canonical.canonicalPath?.let { path -> dao.findRootByCanonicalPath(path) }
        if (owner != null && owner.projectId != projectId) return@runBlocking ProjectRelocation.FolderInUse

        val roots = dao.listRoots().filter { root -> root.projectId == projectId }
        val primary = roots.firstOrNull { root -> root.role == ProjectRootRole.Primary.wireValue } ?: roots.firstOrNull()
        val moved = (primary ?: ProjectRootEntity(
            id = idGenerator(),
            projectId = projectId,
            role = ProjectRootRole.Primary.wireValue,
            lastKnownPath = canonical.lastKnownPath,
            canonicalPath = canonical.canonicalPath,
            availability = canonical.availability.wireValue,
            sortOrder = 0,
        )).copy(
            lastKnownPath = canonical.lastKnownPath,
            canonicalPath = canonical.canonicalPath,
            availability = canonical.availability.wireValue,
        )
        // Worktrees kept inside the Project's folder moved with it.
        val oldFolders = listOfNotNull(primary?.lastKnownPath, primary?.canonicalPath).distinct()
        val taskIds = dao.listTasks().filter { task -> task.projectId == projectId }.map { task -> task.id }.toSet()
        val now = nowEpochMillis()
        val followed = dao.listWorkContexts().filter { workContext -> workContext.taskId in taskIds }.mapNotNull { workContext ->
            val path = workContext.worktreePath ?: return@mapNotNull null
            val oldFolder = oldFolders.firstOrNull { folder -> path.startsWith("$folder/") } ?: return@mapNotNull null
            workContext.copy(worktreePath = moved.lastKnownPath + path.removePrefix(oldFolder), updatedAtEpochMillis = now)
        }
        dao.relocateProject(projectEntity.copy(updatedAtEpochMillis = now), moved, followed)
        ProjectRelocation.Moved
    }

    override fun updateTaskStatus(taskId: String, status: TaskStatus) = runBlocking {
        val dao = database.projectCatalogDao()
        val task = dao.findTask(taskId) ?: return@runBlocking
        if (task.status == status.wireValue) return@runBlocking
        dao.upsertTask(task.copy(status = status.wireValue, updatedAtEpochMillis = nowEpochMillis()))
    }

    override fun setTaskAgent(taskId: String, agent: String?) = runBlocking {
        val dao = database.projectCatalogDao()
        val task = dao.findTask(taskId) ?: return@runBlocking
        dao.upsertTask(task.copy(lastAgent = agent?.takeIf { it.isNotBlank() }, updatedAtEpochMillis = nowEpochMillis()))
    }

    override fun renameTask(taskId: String, title: String) = runBlocking {
        val dao = database.projectCatalogDao()
        val name = title.replace(LineBreaks, " ").trim().take(MaxTaskTitleLength)
        if (name.isEmpty()) return@runBlocking
        val task = dao.findTask(taskId) ?: return@runBlocking
        if (task.title == name) return@runBlocking
        dao.upsertTask(task.copy(title = name, updatedAtEpochMillis = nowEpochMillis()))
    }

    override fun retitleTasks(titles: Map<String, String>) = runBlocking {
        val dao = database.projectCatalogDao()
        titles.forEach { (taskId, title) ->
            val name = title.replace(LineBreaks, " ").trim().take(MaxTaskTitleLength)
            val task = dao.findTask(taskId)
            if (name.isNotEmpty() && task != null && task.title != name) dao.upsertTask(task.copy(title = name))
        }
    }

    override fun deleteTask(taskId: String) = runBlocking {
        val dao = database.projectCatalogDao()
        val task = dao.findTask(taskId) ?: return@runBlocking
        val project = dao.findProject(task.projectId)
        val selection = dao.getSelection()
        dao.deleteTaskCascade(
            taskId = taskId,
            project = project?.takeIf { it.lastSelectedTaskId == taskId }?.copy(lastSelectedTaskId = null, updatedAtEpochMillis = nowEpochMillis()),
            selection = selection?.takeIf { it.selectedTaskId == taskId }?.copy(selectedTaskId = null),
        )
    }

    override fun createTask(projectId: String, title: String, desiredOutcome: String): Task = runBlocking {
        val dao = database.projectCatalogDao()
        val projectEntity = requireNotNull(dao.findProject(projectId)) {
            "Cannot create a Task for unknown Project `$projectId`."
        }
        val now = nowEpochMillis()
        val taskId = idGenerator()
        val revisionId = idGenerator()

        val taskEntity = TaskEntity(
            id = taskId,
            projectId = projectId,
            title = title.trim().ifEmpty { "Untitled task" },
            status = TaskStatus.Understanding.wireValue,
            currentRevisionId = revisionId,
            currentResultId = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        val revisionEntity = TaskRevisionEntity(
            id = revisionId,
            taskId = taskId,
            ordinal = 1,
            desiredOutcome = desiredOutcome.trim(),
            createdAtEpochMillis = now,
        )
        dao.insertTaskWithRevision(
            task = taskEntity,
            revision = revisionEntity,
            project = projectEntity.copy(lastSelectedTaskId = taskId, updatedAtEpochMillis = now),
        )

        ProjectCatalogMappers.taskFrom(taskEntity)
    }

    override fun loadTask(taskId: String): TaskDetail? = runBlocking {
        val dao = database.projectCatalogDao()
        val task = dao.findTask(taskId) ?: return@runBlocking null
        val revision = dao.findRevision(task.currentRevisionId) ?: return@runBlocking null
        ProjectCatalogMappers.detailFrom(task, revision)
    }

    override fun loadWorkContext(taskId: String): WorkContextDescriptor? = runBlocking {
        database.projectCatalogDao().findWorkContext(taskId)?.let { entity -> ProjectCatalogMappers.workContextFrom(entity) }
    }

    override fun saveWorkContext(descriptor: WorkContextDescriptor) {
        runBlocking {
            database.projectCatalogDao().upsertWorkContext(
                ProjectCatalogMappers.workContextEntityFrom(descriptor, nowEpochMillis()),
            )
        }
    }

    override fun updateSelection(selectedProjectId: String?, selectedTaskId: String?) {
        runBlocking {
            val dao = database.projectCatalogDao()
            val project = selectedTaskId
                ?.let { taskId -> selectedProjectId?.let { id -> dao.findProject(id)?.let { entity -> entity to taskId } } }
                ?.let { (entity, taskId) -> entity.copy(lastSelectedTaskId = taskId, updatedAtEpochMillis = nowEpochMillis()) }
            dao.applySelection(
                selection = CatalogSelectionEntity(
                    selectedProjectId = selectedProjectId,
                    selectedTaskId = selectedTaskId,
                ),
                project = project,
            )
        }
    }
}

/** How many closed Projects are offered for reopening; older ones are still restored by opening their folder. */
private const val MaxClosedProjects = 8

private const val MaxProjectNameLength = 80
private const val MaxTaskTitleLength = 120
private val LineBreaks = Regex("\\s*[\\r\\n]+\\s*")
