package app.thdev.moru.core.data

import app.thdev.moru.core.domain.TaskDetail
import app.thdev.moru.core.domain.TaskSummary
import app.thdev.moru.core.model.Project
import app.thdev.moru.core.model.ProjectRoot
import app.thdev.moru.core.model.Task
import app.thdev.moru.core.model.WorkContextDescriptor
import app.thdev.moru.core.model.WorkContextPersistedLifecycle
import app.thdev.moru.core.model.deriveTaskAttention
import app.thdev.moru.core.model.projectAvailabilityFromWireValue
import app.thdev.moru.core.model.projectRootRoleFromWireValue
import app.thdev.moru.core.model.taskStatusFromWireValue
import app.thdev.moru.core.model.wireValue
import app.thdev.moru.core.model.workContextPersistedLifecycleFromWireValue

/** Entity-to-model mapping for the global catalog. One owner keeps the boundary in one place. */
internal object ProjectCatalogMappers {
    fun projectFrom(entity: ProjectEntity, roots: List<ProjectRootEntity>): Project =
        Project(
            id = entity.id,
            displayName = entity.displayName,
            roots = roots.sortedBy { root -> root.sortOrder }.map { root -> rootFrom(root) },
            lastSelectedTaskId = entity.lastSelectedTaskId,
            createdAtEpochMillis = entity.createdAtEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
        )

    fun rootFrom(entity: ProjectRootEntity): ProjectRoot =
        ProjectRoot(
            id = entity.id,
            role = projectRootRoleFromWireValue(entity.role),
            lastKnownPath = entity.lastKnownPath,
            canonicalPath = entity.canonicalPath,
            availability = projectAvailabilityFromWireValue(entity.availability),
        )

    fun taskFrom(entity: TaskEntity): Task =
        Task(
            id = entity.id,
            projectId = entity.projectId,
            title = entity.title,
            status = taskStatusFromWireValue(entity.status),
            currentRevisionId = entity.currentRevisionId,
            currentResultId = entity.currentResultId,
            createdAtEpochMillis = entity.createdAtEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
        )

    fun workContextFrom(entity: WorkContextEntity): WorkContextDescriptor =
        WorkContextDescriptor(
            id = entity.id,
            taskId = entity.taskId,
            persistedLifecycle = workContextPersistedLifecycleFromWireValue(entity.lifecycle),
            branch = entity.branch,
            worktreePath = entity.worktreePath,
        )

    fun workContextEntityFrom(descriptor: WorkContextDescriptor, updatedAtEpochMillis: Long): WorkContextEntity =
        WorkContextEntity(
            taskId = descriptor.taskId,
            id = descriptor.id,
            lifecycle = descriptor.persistedLifecycle.wireValue,
            branch = descriptor.branch,
            worktreePath = descriptor.worktreePath,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )

    fun detailFrom(entity: TaskEntity, revision: TaskRevisionEntity): TaskDetail =
        TaskDetail(
            id = entity.id,
            projectId = entity.projectId,
            title = entity.title,
            status = taskStatusFromWireValue(entity.status),
            desiredOutcome = revision.desiredOutcome,
            revisionOrdinal = revision.ordinal,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
        )

    fun summaryFrom(
        entity: TaskEntity,
        workContextLifecycle: WorkContextPersistedLifecycle? = null,
    ): TaskSummary {
        val taskStatus = taskStatusFromWireValue(entity.status)
        return TaskSummary(
            id = entity.id,
            projectId = entity.projectId,
            title = entity.title,
            status = taskStatus,
            attention = deriveTaskAttention(status = taskStatus, pendingApprovalCount = 0),
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            workContextLifecycle = workContextLifecycle,
            lastAgent = entity.lastAgent,
            createdAtEpochMillis = entity.createdAtEpochMillis,
        )
    }
}
