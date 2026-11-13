package app.thdev.moru.core.domain.catalog

import app.thdev.moru.core.model.ProjectAvailability
import app.thdev.moru.core.model.TaskStatus

/** Pure transitions. The caller supplies identity, canonicalization, time, and durable commit. */
class CatalogPolicy {
    fun validate(snapshot: CatalogSnapshot): String? = CatalogValidation.check(snapshot)

    fun openProject(
        snapshot: CatalogSnapshot, id: String, rootId: String, name: String, canonicalPath: String, nowMillis: Long,
    ): CatalogTransition = mutate(snapshot, nowMillis, listOf(id, rootId, name, canonicalPath)) {
        if (!CatalogValidation.absolutePath(canonicalPath)) return@mutate reject(snapshot, "invalid_path")
        val existing = snapshot.projects.firstOrNull { it.path == canonicalPath }
        if (existing != null) {
            val reopened = if (existing.closed || existing.availability != ProjectAvailability.Available) {
                existing.copy(closed = false, availability = ProjectAvailability.Available, updatedAtMillis = nowMillis)
            } else existing
            accept(snapshot.copy(
                projects = snapshot.projects.map { if (it.id == existing.id) reopened else it },
                selectedProjectId = existing.id, selectedTaskId = existing.lastTaskId,
            ))
        } else {
            val resolvedName = CatalogValidation.name(name, 80).ifEmpty {
                CatalogValidation.name(canonicalPath.substringAfterLast('/'), 80).ifEmpty { "Project" }
            }
            accept(snapshot.copy(
                projects = snapshot.projects + CatalogProject(
                    id, rootId, resolvedName, canonicalPath, ProjectAvailability.Available, null, nowMillis, nowMillis, false,
                ),
                selectedProjectId = id, selectedTaskId = null,
            ))
        }
    }

    fun createTask(
        snapshot: CatalogSnapshot, projectId: String, taskId: String, revisionId: String, description: String, nowMillis: Long,
    ): CatalogTransition = mutate(snapshot, nowMillis, listOf(projectId, taskId, revisionId, description)) {
        val project = snapshot.projects.firstOrNull { it.id == projectId } ?: return@mutate reject(snapshot, "not_found")
        if (project.closed) return@mutate reject(snapshot, "project_closed")
        if (description.encodeToByteArray().size > CatalogValidation.MaxDescriptionBytes) {
            return@mutate reject(snapshot, "description_limit")
        }
        val outcome = description.trim()
        val titles = snapshot.tasks.filter { it.projectId == projectId }.map { it.title }
        if (outcome.isEmpty() && titles.any { numberedTaskNumber(project.name, it) == Int.MAX_VALUE }) {
            return@mutate reject(snapshot, "title_overflow")
        }
        val title = outcome.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.let { CatalogValidation.truncate(it, 80) }
            ?: nextNumberedTaskTitle(project.name, titles)
        accept(snapshot.copy(
            projects = snapshot.projects.map {
                if (it.id == projectId) it.copy(lastTaskId = taskId, updatedAtMillis = nowMillis) else it
            },
            tasks = snapshot.tasks + CatalogTask(
                taskId, projectId, revisionId, title, outcome, TaskStatus.Understanding, nowMillis, nowMillis,
            ),
            selectedProjectId = projectId, selectedTaskId = taskId,
        ))
    }

    fun selectProject(snapshot: CatalogSnapshot, projectId: String): CatalogTransition = mutate(snapshot, inputs = listOf(projectId)) {
        val project = snapshot.projects.firstOrNull { it.id == projectId } ?: return@mutate reject(snapshot, "not_found")
        if (project.closed) return@mutate reject(snapshot, "project_closed")
        accept(snapshot.copy(selectedProjectId = projectId, selectedTaskId = project.lastTaskId))
    }

    fun selectTask(snapshot: CatalogSnapshot, projectId: String, taskId: String): CatalogTransition = mutate(snapshot, inputs = listOf(projectId, taskId)) {
        val project = snapshot.projects.firstOrNull { it.id == projectId } ?: return@mutate reject(snapshot, "not_found")
        if (project.closed) return@mutate reject(snapshot, "project_closed")
        val task = snapshot.tasks.firstOrNull { it.id == taskId } ?: return@mutate reject(snapshot, "not_found")
        if (task.projectId != projectId) return@mutate reject(snapshot, "invalid_owner")
        accept(snapshot.copy(
            projects = snapshot.projects.map { if (it.id == projectId) it.copy(lastTaskId = taskId) else it },
            selectedProjectId = projectId, selectedTaskId = taskId,
        ))
    }

    fun composeTask(snapshot: CatalogSnapshot, projectId: String): CatalogTransition = mutate(snapshot, inputs = listOf(projectId)) {
        val project = snapshot.projects.firstOrNull { it.id == projectId } ?: return@mutate reject(snapshot, "not_found")
        if (project.closed) return@mutate reject(snapshot, "project_closed")
        accept(snapshot.copy(selectedProjectId = projectId, selectedTaskId = null))
    }

    fun renameProject(snapshot: CatalogSnapshot, projectId: String, name: String, nowMillis: Long): CatalogTransition =
        mutate(snapshot, nowMillis, listOf(projectId, name)) {
            val project = snapshot.projects.firstOrNull { it.id == projectId } ?: return@mutate reject(snapshot, "not_found")
            val normalized = CatalogValidation.name(name, 80)
            if (normalized.isEmpty()) return@mutate reject(snapshot, "invalid_name")
            if (normalized == project.name) return@mutate accept(snapshot)
            accept(snapshot.copy(projects = snapshot.projects.map {
                if (it.id == projectId) it.copy(name = normalized, updatedAtMillis = nowMillis) else it
            }))
        }

    fun renameTask(snapshot: CatalogSnapshot, taskId: String, title: String, nowMillis: Long): CatalogTransition =
        mutate(snapshot, nowMillis, listOf(taskId, title)) {
            val task = snapshot.tasks.firstOrNull { it.id == taskId } ?: return@mutate reject(snapshot, "not_found")
            val normalized = CatalogValidation.name(title, 120)
            if (normalized.isEmpty()) return@mutate reject(snapshot, "invalid_name")
            if (normalized == task.title) return@mutate accept(snapshot)
            accept(snapshot.copy(tasks = snapshot.tasks.map {
                if (it.id == taskId) it.copy(title = normalized, updatedAtMillis = nowMillis) else it
            }))
        }

    fun setTaskDone(snapshot: CatalogSnapshot, taskId: String, done: Boolean, nowMillis: Long): CatalogTransition =
        mutate(snapshot, nowMillis, listOf(taskId)) {
            val task = snapshot.tasks.firstOrNull { it.id == taskId } ?: return@mutate reject(snapshot, "not_found")
            val status = if (done) TaskStatus.Done else if (task.status == TaskStatus.Done) TaskStatus.Understanding else task.status
            if (status == task.status) return@mutate accept(snapshot)
            accept(snapshot.copy(tasks = snapshot.tasks.map {
                if (it.id == taskId) it.copy(status = status, updatedAtMillis = nowMillis) else it
            }))
        }

    fun closeProject(snapshot: CatalogSnapshot, projectId: String, nowMillis: Long): CatalogTransition =
        mutate(snapshot, nowMillis, listOf(projectId)) {
            val project = snapshot.projects.firstOrNull { it.id == projectId } ?: return@mutate reject(snapshot, "not_found")
            if (project.closed) return@mutate accept(snapshot)
            val selected = snapshot.selectedProjectId == projectId
            accept(snapshot.copy(
                projects = snapshot.projects.map { if (it.id == projectId) it.copy(closed = true, updatedAtMillis = nowMillis) else it },
                selectedProjectId = if (selected) null else snapshot.selectedProjectId,
                selectedTaskId = if (selected) null else snapshot.selectedTaskId,
            ))
        }

    private fun mutate(
        original: CatalogSnapshot, nowMillis: Long? = null, inputs: List<String> = emptyList(), operation: () -> CatalogTransition,
    ): CatalogTransition {
        validate(original)?.let { return reject(original, it) }
        if (nowMillis != null && nowMillis < 0) return reject(original, "invalid_timestamp")
        if (inputs.any { !CatalogValidation.wellFormed(it) }) return reject(original, "invalid_text")
        val result = operation()
        if (!result.accepted) return reject(original, result.errorCode!!)
        val candidate = result.snapshot
        if (candidate == original) return accept(original)
        validate(candidate)?.let { return reject(original, it) }
        val oldProjects = original.projects.associateBy { it.id }
        val oldTasks = original.tasks.associateBy { it.id }
        if (candidate.projects.any { it.updatedAtMillis < (oldProjects[it.id]?.updatedAtMillis ?: 0) } ||
            candidate.tasks.any { it.updatedAtMillis < (oldTasks[it.id]?.updatedAtMillis ?: 0) }) {
            return reject(original, "stale_timestamp")
        }
        if (original.generation == Long.MAX_VALUE) return reject(original, "generation_overflow")
        return accept(candidate.copy(generation = original.generation + 1))
    }

    private fun accept(snapshot: CatalogSnapshot) = CatalogTransition(snapshot, null)
    private fun reject(snapshot: CatalogSnapshot, code: String) = CatalogTransition(snapshot, code)
}
