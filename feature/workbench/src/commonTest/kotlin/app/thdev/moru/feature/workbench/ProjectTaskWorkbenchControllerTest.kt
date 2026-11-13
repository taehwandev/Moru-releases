package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.domain.ProjectCatalogSnapshot
import app.thdev.moru.core.domain.ProjectEntry
import app.thdev.moru.core.domain.ProjectRegistration
import app.thdev.moru.core.domain.ProjectRelocation
import app.thdev.moru.core.domain.TaskDetail
import app.thdev.moru.core.domain.TaskSummary
import app.thdev.moru.core.model.GeneralSettings
import app.thdev.moru.core.model.Project
import app.thdev.moru.core.model.ProjectAvailability
import app.thdev.moru.core.model.ProjectRoot
import app.thdev.moru.core.model.ProjectRootRole
import app.thdev.moru.core.model.Task
import app.thdev.moru.core.model.TaskStatus
import app.thdev.moru.core.model.WorkContextDescriptor
import app.thdev.moru.core.model.deriveTaskAttention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectTaskWorkbenchControllerTest {
    @Test
    fun theNextLaunchNumbersTasksEarlierVersionsNamedAfterTheirAgent() {
        val repository = FakeProjectCatalogRepository()
        val first = ProjectTaskWorkbenchController(repository)
        val projectId = first.openProject("/work/alpha").projects.single().id
        first.createTask(projectId, "Codex session", "")
        first.createTask(projectId, "Fix login", "")

        val titles = ProjectTaskWorkbenchController(repository).currentState().projects.single().tasks.map { it.title }

        assertEquals(listOf("Fix login", "alpha task 1"), titles.sorted())
    }

    @Test
    fun relocatingAProjectKeepsItsTasksAndShowsTheNewFolder() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val projectId = controller.openProject("/work/alpha").projects.single().id
        controller.createTask(projectId, "Fix login", "")

        assertEquals(ProjectRelocation.Moved, controller.relocateProject(projectId, "/work/alpha-renamed"))

        val project = controller.currentState().projects.single()
        assertEquals(projectId, project.id)
        assertEquals("/work/alpha-renamed", project.rootPath)
        assertEquals(listOf("Fix login"), project.tasks.map { it.title })
    }

    @Test
    fun openProjectRegistersSelectsAndExpandsIt() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())

        val state = controller.openProject("/work/alpha")

        val row = state.projects.single()
        assertEquals("alpha", row.displayName)
        assertTrue(row.selected)
        assertTrue(row.expanded)
        assertEquals(row.id, state.selectedProjectId)
    }

    @Test
    fun createTaskAddsAndSelectsTheTaskUnderItsProject() {
        val repository = FakeProjectCatalogRepository()
        val controller = ProjectTaskWorkbenchController(repository)
        val projectId = controller.openProject("/work/alpha").projects.single().id

        val state = controller.createTask(projectId, "Fix login", "Login broken")

        val row = state.projects.single()
        assertEquals(listOf("Fix login"), row.tasks.map { task -> task.title })
        assertTrue(row.tasks.single().selected)
        assertEquals(row.tasks.single().id, state.selectedTaskId)
    }

    @Test
    fun toggleProjectCollapsesTheSelectedProjectAndSelectingItExpandsItAgain() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val projectId = controller.openProject("/work/alpha").projects.single().id

        assertFalse(controller.toggleProject(projectId).projects.single().expanded)
        assertTrue(controller.selectProject(projectId).projects.single().expanded)
    }

    @Test
    fun aTaskStartedWithoutADescriptionIsNumberedAfterItsProject() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val projectId = controller.openProject("/work/alpha").projects.single().id

        controller.createTaskFromDescription(projectId, "")
        val state = controller.createTaskFromDescription(projectId, "  \n ")

        assertEquals(listOf("alpha task 1", "alpha task 2"), state.projects.single().tasks.map { task -> task.title }.sorted())
    }

    @Test
    fun onLaunchEveryProjectIsExpandedUnlessTheUserCollapsedIt() {
        val repository = FakeProjectCatalogRepository()
        val first = ProjectTaskWorkbenchController(repository)
        first.openProject("/work/alpha")
        val betaId = first.openProject("/work/beta").projects.first { row -> row.displayName == "beta" }.id
        first.openProject("/work/gamma")

        assertTrue(ProjectTaskWorkbenchController(repository).currentState().projects.all { row -> row.expanded })
        val relaunched = ProjectTaskWorkbenchController(repository, initiallyCollapsed = setOf(betaId)).currentState()
        assertEquals(listOf("alpha", "gamma"), relaunched.projects.filter { row -> row.expanded }.map { row -> row.displayName })
    }

    @Test
    fun toggleProjectCollapsesAndExpandsAnUnselectedProject() {
        val repository = FakeProjectCatalogRepository()
        val controller = ProjectTaskWorkbenchController(repository)
        controller.openProject("/work/alpha")
        // Opening beta selects beta; alpha stays expanded from when it was opened.
        val alphaId = controller.openProject("/work/beta").projects.first { row -> row.displayName == "alpha" }.id

        // alpha is not selected, so toggling collapses it, then expands it again.
        val collapsed = controller.toggleProject(alphaId).projects.single { row -> row.id == alphaId }
        assertFalse(collapsed.expanded)
        val expanded = controller.toggleProject(alphaId).projects.single { row -> row.id == alphaId }
        assertTrue(expanded.expanded)
    }

    @Test
    fun createTaskFromDescriptionUsesFirstNonBlankLineAsTitle() {
        val repository = FakeProjectCatalogRepository()
        val controller = ProjectTaskWorkbenchController(repository)
        val projectId = controller.openProject("/work/alpha").projects.single().id

        val state = controller.createTaskFromDescription(
            projectId,
            "\nFix the login redirect\nwhen the password is invalid",
        )

        val task = state.projects.single().tasks.single()
        assertEquals("Fix the login redirect", task.title)
        assertTrue(task.selected)
    }

    @Test
    fun taskDetailReturnsTheRequestTextForACreatedTask() {
        val repository = FakeProjectCatalogRepository()
        val controller = ProjectTaskWorkbenchController(repository)
        val projectId = controller.openProject("/work/alpha").projects.single().id
        val taskId = controller.createTaskFromDescription(projectId, "Fix the login redirect").selectedTaskId!!

        val detail = controller.taskDetail(taskId)

        assertEquals("Fix the login redirect", detail?.desiredOutcome)
        assertEquals("Fix the login redirect", detail?.title)
    }

    @Test
    fun startNewTaskClearsTaskSelectionToShowTheComposer() {
        val repository = FakeProjectCatalogRepository()
        val controller = ProjectTaskWorkbenchController(repository)
        val projectId = controller.openProject("/work/alpha").projects.single().id
        controller.createTask(projectId, "Existing", "")

        val state = controller.startNewTask(projectId)

        assertEquals(projectId, state.selectedProjectId)
        assertNull(state.selectedTaskId)
        assertTrue(state.projects.single { row -> row.id == projectId }.expanded)
    }

    @Test
    fun selectingATaskPersistsAHibernatedWorkContextShownInTheSidebar() {
        val repository = FakeProjectCatalogRepository()
        val controller = ProjectTaskWorkbenchController(repository)
        val projectId = controller.openProject("/work/alpha").projects.single().id
        val taskId = controller.createTaskFromDescription(projectId, "Fix login").selectedTaskId!!

        // createTask already opens the context; selecting again is idempotent.
        val state = controller.selectTask(projectId, taskId)

        assertEquals(taskId, repository.loadWorkContext(taskId)?.taskId)
        assertTrue(state.projects.single().tasks.single { row -> row.id == taskId }.hasWorkContext)
    }

    @Test
    fun closingAProjectRemovesItFromTheListAndClearsItsSelection() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val keep = controller.openProject("/work/keep").projects.single().id
        val closing = controller.openProject("/work/closing").projects.last().id

        val state = controller.closeProject(closing)

        assertEquals(listOf(keep), state.projects.map { project -> project.id })
        assertEquals(null, state.selectedProjectId)
    }

    @Test
    fun openingTheFolderOfAClosedProjectBringsItBackWithItsTasks() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val id = controller.openProject("/work/alpha").projects.single().id
        controller.createTask(id, "Keep me", "")
        controller.closeProject(id)

        val state = controller.openProject("/work/alpha")

        assertEquals(listOf("Keep me"), state.projects.single().tasks.map { task -> task.title })
    }

    @Test
    fun renamingAProjectChangesItsNameAndKeepsItsTasks() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val id = controller.openProject("/work/alpha").projects.single().id
        controller.createTask(id, "Keep me", "")

        val state = controller.renameProject(id, "  Alpha app  ")

        assertEquals("Alpha app", state.projects.single().displayName)
        assertEquals(listOf("Keep me"), state.projects.single().tasks.map { task -> task.title })
        assertEquals("Alpha app", controller.renameProject(id, "   ").projects.single().displayName)
    }

    @Test
    fun markingATaskDoneIsKeptAndReopeningBringsItBack() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val projectId = controller.openProject("/work/alpha").projects.single().id
        val taskId = controller.createTask(projectId, "Fix login", "").selectedTaskId!!

        val done = controller.markTaskDone(taskId).projects.single().tasks.single()
        assertEquals(TaskDisplayState.Done, done.displayState)

        val reopened = controller.reopenTask(taskId).projects.single().tasks.single()
        assertTrue(reopened.displayState != TaskDisplayState.Done)
    }

    @Test
    fun renamingATaskChangesItsTitleAndABlankTitleChangesNothing() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val projectId = controller.openProject("/work/alpha").projects.single().id
        val taskId = controller.createTask(projectId, "Fix login", "").selectedTaskId!!

        assertEquals("Fix signup", controller.renameTask(taskId, "  Fix signup ").projects.single().tasks.single().title)
        assertEquals("Fix signup", controller.renameTask(taskId, "   ").projects.single().tasks.single().title)
    }

    @Test
    fun aNumberedTaskIsNamedOnceAfterItsFirstSessionsSummary() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val projectId = controller.openProject("/work/alpha").projects.single().id
        val numbered = controller.createTaskFromDescription(projectId, "").selectedTaskId!!
        val described = controller.createTaskFromDescription(projectId, "Fix login").selectedTaskId!!

        val named = controller.nameTasksFromSessions(mapOf(numbered to "서비스 안정성 문제", described to "Other"))

        assertEquals(listOf("서비스 안정성 문제", "Fix login"), named?.projects?.single()?.tasks?.map { it.title }?.sortedDescending())
        assertNull(controller.nameTasksFromSessions(mapOf(numbered to "A later summary")))
    }

    @Test
    fun deletingATaskRemovesItAndClearsTheSelectionThatNamedIt() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val projectId = controller.openProject("/work/alpha").projects.single().id
        val gone = controller.createTask(projectId, "Gone", "").selectedTaskId!!
        controller.createTask(projectId, "Stays", "")
        controller.selectTask(projectId, gone)

        val state = controller.deleteTask(gone)

        assertEquals(listOf("Stays"), state.projects.single().tasks.map { task -> task.title })
        assertEquals(null, state.selectedTaskId)
    }

    @Test
    fun selectTaskUpdatesSelection() {
        val repository = FakeProjectCatalogRepository()
        val controller = ProjectTaskWorkbenchController(repository)
        val projectId = controller.openProject("/work/alpha").projects.single().id
        val taskId = controller.createTask(projectId, "Task A", "").selectedTaskId!!
        controller.createTask(projectId, "Task B", "")

        val state = controller.selectTask(projectId, taskId)

        assertEquals(taskId, state.selectedTaskId)
    }

    @Test
    fun deletingAClosedProjectDropsItsTasksAndOpeningTheFolderAgainStartsFresh() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val projectId = controller.openProject("/work/alpha").projects.single().id
        controller.createTask(projectId, "Old task", "")
        controller.closeProject(projectId)

        controller.forgetClosedProject(projectId)
        val reopened = controller.openProject("/work/alpha")

        assertTrue(reopened.projects.single().tasks.isEmpty())
    }

    @Test
    fun byDefaultSelectingAProjectOrTaskKeepsOtherProjectsExpanded() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val alphaId = controller.openProject("/work/alpha").projects.single().id
        controller.createTask(alphaId, "Alpha task", "")
        val betaId = controller.openProject("/work/beta").projects.first { it.displayName == "beta" }.id
        val betaTaskId = controller.createTask(betaId, "Beta task", "").selectedTaskId!!

        val afterSelectAlpha = controller.selectProject(alphaId)
        assertFalse(afterSelectAlpha.autoCollapseInactiveSidebar)
        assertTrue(afterSelectAlpha.projects.single { it.id == alphaId }.expanded)
        assertTrue(afterSelectAlpha.projects.single { it.id == betaId }.expanded)

        val afterSelectBetaTask = controller.selectTask(betaId, betaTaskId)
        assertTrue(afterSelectBetaTask.projects.single { it.id == alphaId }.expanded)
        assertTrue(afterSelectBetaTask.projects.single { it.id == betaId }.expanded)
    }

    @Test
    fun whenAutoCollapseIsEnabledSelectingAProjectOrTaskCollapsesOtherProjectsAndDisablingRestoresThem() {
        val controller = ProjectTaskWorkbenchController(FakeProjectCatalogRepository())
        val alphaId = controller.openProject("/work/alpha").projects.single().id
        val alphaTaskId = controller.createTask(alphaId, "Alpha task", "").selectedTaskId!!
        val betaId = controller.openProject("/work/beta").projects.first { it.displayName == "beta" }.id
        val betaTaskId = controller.createTask(betaId, "Beta task", "").selectedTaskId!!

        val enabled = controller.applyGeneralSettings(GeneralSettings(autoCollapseInactiveSidebar = true))
        assertTrue(enabled.autoCollapseInactiveSidebar)
        assertFalse(enabled.projects.single { it.id == alphaId }.expanded)
        assertTrue(enabled.projects.single { it.id == betaId }.expanded)

        val afterSelectAlpha = controller.selectProject(alphaId)
        assertTrue(afterSelectAlpha.projects.single { it.id == alphaId }.expanded)
        assertFalse(afterSelectAlpha.projects.single { it.id == betaId }.expanded)

        val afterSelectBetaTask = controller.selectTask(betaId, betaTaskId)
        assertFalse(afterSelectBetaTask.projects.single { it.id == alphaId }.expanded)
        assertTrue(afterSelectBetaTask.projects.single { it.id == betaId }.expanded)

        val afterSelectAlphaTask = controller.selectTask(alphaId, alphaTaskId)
        assertTrue(afterSelectAlphaTask.projects.single { it.id == alphaId }.expanded)
        assertFalse(afterSelectAlphaTask.projects.single { it.id == betaId }.expanded)

        val disabled = controller.applyGeneralSettings(GeneralSettings(autoCollapseInactiveSidebar = false))
        assertFalse(disabled.autoCollapseInactiveSidebar)
        assertTrue(disabled.projects.all { it.expanded })
    }
}

private class FakeProjectCatalogRepository : ProjectCatalogRepository {
    private data class TaskRecord(val task: Task, val desiredOutcome: String)

    private val projects = mutableListOf<Project>()
    private val tasksByProject = mutableMapOf<String, MutableList<TaskRecord>>()
    private var selectedProjectId: String? = null
    private var selectedTaskId: String? = null
    private var counter = 0
    private val closedIds = mutableSetOf<String>()

    override fun loadCatalog(): ProjectCatalogSnapshot =
        ProjectCatalogSnapshot(
            projects = projects.filter { project -> project.id !in closedIds }.map { project ->
                ProjectEntry(
                    project = project,
                    tasks = tasksByProject[project.id].orEmpty().map { record ->
                        TaskSummary(
                            id = record.task.id,
                            projectId = project.id,
                            title = record.task.title,
                            status = record.task.status,
                            attention = deriveTaskAttention(record.task.status, pendingApprovalCount = 0),
                            updatedAtEpochMillis = record.task.updatedAtEpochMillis,
                            workContextLifecycle = workContexts[record.task.id]?.persistedLifecycle,
                        )
                    },
                )
            },
            selectedProjectId = selectedProjectId,
            selectedTaskId = selectedTaskId,
        )

    override fun registerProject(displayName: String, rootPath: String): ProjectRegistration {
        val canonical = rootPath.trimEnd('/')
        projects.firstOrNull { project -> project.primaryRoot?.canonicalPath == canonical }?.let { existing ->
            closedIds -= existing.id
            return ProjectRegistration(project = existing, alreadyRegistered = true)
        }
        val id = "p${++counter}"
        val name = displayName.ifBlank { canonical.substringAfterLast('/') }
        val project = Project(
            id = id,
            displayName = name,
            roots = listOf(
                ProjectRoot(
                    id = "$id-root",
                    role = ProjectRootRole.Primary,
                    lastKnownPath = rootPath,
                    canonicalPath = canonical,
                    availability = ProjectAvailability.Available,
                ),
            ),
            createdAtEpochMillis = 0,
            updatedAtEpochMillis = 0,
        )
        projects += project
        return ProjectRegistration(project = project, alreadyRegistered = false)
    }

    override fun closeProject(projectId: String) {
        closedIds += projectId
        if (selectedProjectId == projectId) {
            selectedProjectId = null
            selectedTaskId = null
        }
    }

    override fun forgetClosedProject(projectId: String) {
        if (projectId !in closedIds) return
        projects.removeAll { project -> project.id == projectId }
        tasksByProject.remove(projectId)
        closedIds -= projectId
    }

    override fun relocateProject(projectId: String, rootPath: String): ProjectRelocation {
        val index = projects.indexOfFirst { project -> project.id == projectId }
        if (index < 0) return ProjectRelocation.NotFound
        val canonical = rootPath.trimEnd('/')
        if (projects.any { project -> project.id != projectId && project.primaryRoot?.canonicalPath == canonical }) {
            return ProjectRelocation.FolderInUse
        }
        projects[index] = projects[index].copy(roots = projects[index].roots.map { root ->
            root.copy(lastKnownPath = rootPath, canonicalPath = canonical)
        })
        return ProjectRelocation.Moved
    }

    override fun renameProject(projectId: String, displayName: String) {
        val name = displayName.trim()
        if (name.isEmpty()) return
        val index = projects.indexOfFirst { project -> project.id == projectId }
        if (index >= 0) projects[index] = projects[index].copy(displayName = name)
    }

    private fun changeTask(taskId: String, change: (Task) -> Task) {
        tasksByProject.values.forEach { records ->
            val index = records.indexOfFirst { record -> record.task.id == taskId }
            if (index >= 0) records[index] = records[index].copy(task = change(records[index].task))
        }
    }

    override fun updateTaskStatus(taskId: String, status: TaskStatus) = changeTask(taskId) { it.copy(status = status) }

    override fun setTaskAgent(taskId: String, agent: String?) = changeTask(taskId) { it }

    override fun renameTask(taskId: String, title: String) {
        if (title.isNotBlank()) changeTask(taskId) { it.copy(title = title.trim()) }
    }

    override fun deleteTask(taskId: String) {
        tasksByProject.values.forEach { records -> records.removeAll { record -> record.task.id == taskId } }
        if (selectedTaskId == taskId) selectedTaskId = null
    }

    override fun createTask(projectId: String, title: String, desiredOutcome: String): Task {
        val id = "t${++counter}"
        val task = Task(
            id = id,
            projectId = projectId,
            title = title,
            status = TaskStatus.Understanding,
            currentRevisionId = "$id-rev",
            createdAtEpochMillis = 0,
            updatedAtEpochMillis = 0,
        )
        tasksByProject.getOrPut(projectId) { mutableListOf() }.add(TaskRecord(task, desiredOutcome))
        return task
    }

    override fun loadTask(taskId: String): TaskDetail? {
        val record = tasksByProject.values.flatten().firstOrNull { record -> record.task.id == taskId }
            ?: return null
        return TaskDetail(
            id = record.task.id,
            projectId = record.task.projectId,
            title = record.task.title,
            status = record.task.status,
            desiredOutcome = record.desiredOutcome,
            revisionOrdinal = 1,
            updatedAtEpochMillis = record.task.updatedAtEpochMillis,
        )
    }

    private val workContexts = mutableMapOf<String, WorkContextDescriptor>()

    override fun loadWorkContext(taskId: String): WorkContextDescriptor? = workContexts[taskId]

    override fun saveWorkContext(descriptor: WorkContextDescriptor) {
        workContexts[descriptor.taskId] = descriptor
    }

    override fun updateSelection(selectedProjectId: String?, selectedTaskId: String?) {
        this.selectedProjectId = selectedProjectId
        this.selectedTaskId = selectedTaskId
        if (selectedProjectId != null) {
            val index = projects.indexOfFirst { project -> project.id == selectedProjectId }
            if (index >= 0) {
                projects[index] = projects[index].copy(lastSelectedTaskId = selectedTaskId)
            }
        }
    }
}
