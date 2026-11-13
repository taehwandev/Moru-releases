package app.thdev.moru.app

import app.thdev.moru.core.data.*
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import java.nio.file.Files
import kotlin.test.*

class WorkbenchNavigationTest {
    @Test
    fun savedOrderChangesKeepSelectionComposerAndTerminalsAndDriveKeyboardNavigation() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(root.path)
        val projectId = navigation.sidebar.selectedProjectId!!
        val sessions = InMemoryTerminalSessionManager()
        repeat(3) { index ->
            navigation.newTask(projectId)
            val launch = navigation.beginLaunch("Task $index", AgentRuntime.Codex)!!
            navigation.prepare(launch)
            navigation.completeLaunch(launch, sessions, "zsh")
        }
        val order = navigation.selectedProject!!.tasks.map { it.id }
        navigation.selectTask(projectId, order.first())
        val retained = sessions.listSessions()
        val revision = sessions.revision()
        val manual = app.thdev.moru.core.model.GeneralSettings(
            taskSortOrder = app.thdev.moru.core.model.TaskSortOrder.Manual,
            taskOrder = mapOf(projectId to order.reversed()),
        )
        navigation.applyGeneralSettings(manual)
        assertEquals(order.reversed(), navigation.selectedProject!!.tasks.map { it.id })
        assertEquals(order.first(), navigation.sidebar.selectedTaskId)
        navigation.selectAdjacentTask(1)
        assertEquals(order.last(), navigation.sidebar.selectedTaskId, "Keyboard wraps in the displayed order")
        navigation.renameTask(order.first(), "Renamed")
        assertEquals(order.reversed(), navigation.selectedProject!!.tasks.map { it.id })
        navigation.newTask(projectId)
        navigation.applyGeneralSettings(manual.copy(taskSortOrder = app.thdev.moru.core.model.TaskSortOrder.NewestFirst))
        assertEquals(projectId, navigation.composingProjectId)
        assertNull(navigation.sidebar.selectedTaskId)
        assertEquals(retained, sessions.listSessions())
        assertEquals(revision, sessions.revision())
        val restored = WorkbenchNavigation(repository, initialGeneral = manual)
        assertEquals(order.reversed(), restored.sidebar.projects.single().tasks.map { it.id })
    }

    @Test
    fun switchingProjectsRestoresEachLastTaskAndKeepsItsOriginalTerminalSessions() = withRepository { repository, root ->
        val alpha = root.resolve("Alpha").apply { mkdir() }
        val beta = root.resolve("Beta").apply { mkdir() }
        val navigation = WorkbenchNavigation(repository)
        val sessions = InMemoryTerminalSessionManager()

        fun launch(projectId: String, description: String): TerminalSessionSnapshot {
            navigation.newTask(projectId)
            val captured = navigation.beginLaunch(description, AgentRuntime.Claude)!!
            assertTrue(navigation.prepare(captured))
            navigation.completeLaunch(captured, sessions, "zsh")
            return sessions.listSessions().single { it.spec.attachedItemId == captured.taskId }
        }

        navigation.openProject(alpha.path)
        val alphaId = navigation.sidebar.selectedProjectId!!
        val alphaFirst = launch(alphaId, "First Alpha task")
        val alphaSecond = launch(alphaId, "Second Alpha task")
        navigation.selectTask(alphaId, alphaFirst.spec.attachedItemId!!)
        navigation.openProject(beta.path)
        val betaId = navigation.sidebar.selectedProjectId!!
        val betaFirst = launch(betaId, "First Beta task")
        val betaLast = launch(betaId, "Second Beta task")
        sessions.listSessions().forEach { session -> sessions.writeInput(session.id, "echo ${session.id}") }
        val retained = sessions.listSessions()
        val retainedRevision = sessions.revision()
        assertEquals(4, retained.map { it.id }.toSet().size)
        assertEquals(4, retained.map { it.spec.attachedItemId }.toSet().size)
        assertEquals(mapOf(
            alphaFirst.id to alpha.canonicalPath,
            alphaSecond.id to alpha.canonicalPath,
            betaFirst.id to beta.canonicalPath,
            betaLast.id to beta.canonicalPath,
        ), retained.associate { it.id to it.spec.workingDirectory })

        repeat(3) {
            navigation.selectProject(alphaId)
            assertEquals(alphaFirst.spec.attachedItemId, navigation.sidebar.selectedTaskId)
            assertEquals(alpha.canonicalPath, navigation.executionFolder(navigation.sidebar.selectedTaskId, sessions))
            navigation.selectProject(betaId)
            assertEquals(betaLast.spec.attachedItemId, navigation.sidebar.selectedTaskId)
            assertEquals(beta.canonicalPath, navigation.executionFolder(navigation.sidebar.selectedTaskId, sessions))
        }

        navigation.openProject(alpha.path)
        assertEquals(alphaId, navigation.sidebar.selectedProjectId)
        assertEquals(alphaFirst.spec.attachedItemId, navigation.sidebar.selectedTaskId)
        val restoredNavigation = WorkbenchNavigation(repository)
        assertEquals(alphaFirst.spec.attachedItemId, restoredNavigation.sidebar.selectedTaskId)
        assertEquals(alpha.canonicalPath,
            restoredNavigation.executionFolder(restoredNavigation.sidebar.selectedTaskId, sessions),
            "Restored navigation finds the original session without its transient folder map")
        restoredNavigation.selectProject(betaId)
        assertEquals(betaLast.spec.attachedItemId, restoredNavigation.sidebar.selectedTaskId)
        assertEquals(beta.canonicalPath,
            restoredNavigation.executionFolder(restoredNavigation.sidebar.selectedTaskId, sessions))
        assertEquals(retained, sessions.listSessions(), "Switching preserves IDs, folders, and each terminal's input history")
        assertEquals(retainedRevision, sessions.revision(), "Navigation must not create, close, or retarget sessions")
        val projects = repository.loadCatalog().projects
        assertEquals(2, projects.size)
        assertTrue(projects.all { it.tasks.size == 2 }, "Reopening a folder must not create duplicate Tasks")
    }

    @Test
    fun delayedLaunchKeepsItsProjectTaskAndFolderWhenSelectionChanges() = withRepository { repository, root ->
        val alpha = root.resolve("Alpha").apply { mkdir() }
        val beta = root.resolve("Beta").apply { mkdir() }
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(alpha.path)
        val alphaId = navigation.sidebar.selectedProjectId!!
        navigation.newTask(alphaId)
        val captured = navigation.beginLaunch("Fix login", AgentRuntime.Codex)!!
        navigation.openProject(beta.path)
        val betaId = navigation.sidebar.selectedProjectId!!
        navigation.newTask(betaId)
        val betaTask = navigation.beginLaunch("Update docs", AgentRuntime.Claude)!!
        val sessions = InMemoryTerminalSessionManager()

        var startedSession: app.thdev.moru.core.terminal.TerminalSessionSnapshot? = null
        navigation.completeLaunch(captured, sessions, "zsh") {
            startedSession = it
        }

        val session = sessions.listSessions().single()
        assertEquals(captured.taskId, session.spec.attachedItemId)
        assertEquals(captured.projectRoot, session.spec.workingDirectory)
        assertEquals(session, startedSession, "Captured Task starts even while another Task is selected")
        assertEquals(betaId, navigation.sidebar.selectedProjectId)
        assertEquals(betaTask.taskId, navigation.sidebar.selectedTaskId)
        assertTrue(session.spec.startupCommand!!.contains("Fix login"))
        assertFalse(session.spec.startupCommand!!.contains("Update docs"))
    }

    @Test
    fun aSuccessfulLaunchRemembersTheAgentAndAFailedOneDoesNot() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(root.path)
        val projectId = navigation.sidebar.selectedProjectId!!
        navigation.newTask(projectId)
        val captured = navigation.beginLaunch("", AgentRuntime.Codex)!!
        val sessions = InMemoryTerminalSessionManager()

        navigation.prepare(captured)
        navigation.completeLaunch(captured, sessions, "zsh") { error("PTY could not start") }
        assertNull(navigation.sidebar.lastUsedAgent)
        assertNull(navigation.sidebar.projects.single().tasks.single().lastAgent)

        navigation.prepare(captured)
        navigation.completeLaunch(captured, sessions, "zsh")

        assertEquals("Codex", navigation.sidebar.projects.single().tasks.single().lastAgent)
        assertEquals("Codex", navigation.sidebar.lastUsedAgent)
        assertEquals("Codex", repository.loadCatalog().projects.single().tasks.single().lastAgent)
    }

    @Test
    fun nextAndPreviousTaskWalkTheOpenProjectsTasksAndWrapRound() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(root.path)
        val projectId = navigation.sidebar.selectedProjectId!!
        val first = repository.createTask(projectId, "First", "").id
        val second = repository.createTask(projectId, "Second", "").id
        val third = repository.createTask(projectId, "Third", "").id
        navigation.selectProject(projectId)
        val order = navigation.selectedProject!!.tasks.map { it.id }
        assertEquals(setOf(first, second, third), order.toSet())

        navigation.selectTask(projectId, order[0])
        navigation.selectAdjacentTask(1)
        assertEquals(order[1], navigation.sidebar.selectedTaskId)
        navigation.selectAdjacentTask(1)
        navigation.selectAdjacentTask(1)
        assertEquals(order[0], navigation.sidebar.selectedTaskId, "wraps past the last")
        navigation.selectAdjacentTask(-1)
        assertEquals(order[2], navigation.sidebar.selectedTaskId, "wraps before the first")
    }

    @Test
    fun nextAndPreviousProjectWalkTheSidebarOrderAndWrapRound() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        listOf("a", "b", "c").forEach { name -> navigation.openProject(root.resolve(name).apply { mkdir() }.path) }
        val order = navigation.sidebar.projects.map { it.id }
        assertEquals(3, order.size)

        navigation.selectProject(order[0])
        navigation.tab = ProjectTab.Git
        navigation.selectAdjacentProject(1)
        assertEquals(order[1], navigation.sidebar.selectedProjectId)
        assertEquals(ProjectTab.Work, navigation.tab, "opens the way a sidebar click does")
        navigation.selectAdjacentProject(1)
        navigation.selectAdjacentProject(1)
        assertEquals(order[0], navigation.sidebar.selectedProjectId, "wraps past the last")
        navigation.selectAdjacentProject(-1)
        assertEquals(order[2], navigation.sidebar.selectedProjectId, "wraps before the first")
    }

    @Test
    fun movingBetweenTasksKeepsTheOpenCodeOrGitView() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(root.path)
        val projectId = navigation.sidebar.selectedProjectId!!
        val first = repository.createTask(projectId, "First", "").id
        val second = repository.createTask(projectId, "Second", "").id
        navigation.selectProject(projectId)
        navigation.selectTask(projectId, first)

        navigation.tab = ProjectTab.Code
        navigation.selectTask(projectId, second)
        assertEquals(ProjectTab.Code, navigation.tab)
        navigation.tab = ProjectTab.Git
        navigation.selectAdjacentTask(1)
        assertEquals(ProjectTab.Git, navigation.tab)
        assertEquals(first, navigation.sidebar.selectedTaskId)
    }

    @Test
    fun withNoTaskSelectedForwardStartsAtTheFirstAndBackwardAtTheLastAndAnEmptyProjectDoesNothing() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(root.path)
        val projectId = navigation.sidebar.selectedProjectId!!
        navigation.selectAdjacentTask(1)
        assertNull(navigation.sidebar.selectedTaskId)

        repository.createTask(projectId, "One", "")
        repository.createTask(projectId, "Two", "")
        navigation.selectProject(projectId)
        navigation.newTask(projectId)
        val order = navigation.selectedProject!!.tasks.map { it.id }

        navigation.selectAdjacentTask(1)
        assertEquals(order.first(), navigation.sidebar.selectedTaskId)
        navigation.newTask(projectId)
        navigation.selectAdjacentTask(-1)
        assertEquals(order.last(), navigation.sidebar.selectedTaskId)
    }

    @Test
    fun aTerminalThatCannotStartLeavesNoSessionAndARetryReusesTheTask() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(root.path)
        val projectId = navigation.sidebar.selectedProjectId!!
        navigation.newTask(projectId)
        val captured = navigation.beginLaunch("Fix retry", AgentRuntime.Claude)!!
        val sessions = InMemoryTerminalSessionManager()

        navigation.prepare(captured)
        navigation.completeLaunch(captured, sessions, "zsh") { error("PTY could not start") }

        assertTrue(sessions.listSessions().isEmpty())
        assertEquals(captured, navigation.failedLaunches[captured.taskId])
        navigation.prepare(captured)
        navigation.completeLaunch(captured, sessions, "zsh")
        assertEquals(root.canonicalPath, sessions.listSessions().single().spec.workingDirectory)
        assertEquals(captured.taskId, sessions.listSessions().single().spec.attachedItemId)
        assertEquals(1, repository.loadCatalog().projects.single().tasks.size)
        assertTrue(navigation.failedLaunches.isEmpty())
    }

    @Test
    fun newTaskFromAnotherProjectSelectsThatProjectBeforeLaunch() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        val alpha = root.resolve("Alpha").apply { mkdir() }
        val beta = root.resolve("Beta").apply { mkdir() }
        navigation.openProject(alpha.path)
        val alphaId = navigation.sidebar.selectedProjectId!!
        navigation.openProject(beta.path)
        navigation.newTask(alphaId)
        val captured = navigation.beginLaunch("Change Alpha", AgentRuntime.Claude)!!
        assertEquals(alphaId, captured.projectId)
        assertEquals(alpha.canonicalPath, captured.projectRoot)
        assertEquals(alphaId, navigation.sidebar.selectedProjectId)
    }

    @Test
    fun rapidRepeatedSubmissionConsumesOneComposerAndCreatesOneTask() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(root.path)
        val projectId = navigation.sidebar.selectedProjectId!!
        navigation.newTask(projectId)

        val first = navigation.beginLaunch("One request", AgentRuntime.Claude)!!
        assertNull(navigation.beginLaunch("One request", AgentRuntime.Claude))
        assertEquals(1, repository.loadCatalog().projects.single().tasks.size)
        assertTrue(navigation.prepare(first))
        assertFalse(navigation.prepare(first), "A repeated retry must not launch the same Task twice")
        val sessions = InMemoryTerminalSessionManager()
        navigation.completeLaunch(first, sessions, "zsh")
        assertFalse(navigation.prepare(first), "A stale retry after success must not launch the agent again")
        assertEquals(1, sessions.listSessions().size)

        navigation.newTask(projectId)
        assertNotNull(navigation.beginLaunch("Another request", AgentRuntime.Claude))
        assertEquals(2, repository.loadCatalog().projects.single().tasks.size)
    }

    @Test
    fun theTasksFolderIsTheProjectFolderAndSurvivesClosingItsTerminal() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(root.path)
        navigation.newTask(navigation.sidebar.selectedProjectId!!)
        val captured = navigation.beginLaunch("Fix it", AgentRuntime.Claude)!!
        val sessions = InMemoryTerminalSessionManager()
        navigation.prepare(captured)
        navigation.completeLaunch(captured, sessions, "zsh")

        assertEquals(root.canonicalPath, navigation.executionFolder(captured.taskId, sessions))
        assertEquals(root.canonicalPath, sessions.listSessions().single().spec.workingDirectory)
        sessions.closeSession(sessions.listSessions().single().id)
        assertEquals(root.canonicalPath, navigation.executionFolder(captured.taskId, sessions),
            "Closing a terminal must preserve the Task's folder")
    }

    @Test
    fun launchDirectCreatesTaskAndStartsAtOnceWithoutComposing() = withRepository { repository, root ->
        val navigation = WorkbenchNavigation(repository)
        navigation.openProject(root.path)
        val projectId = navigation.sidebar.selectedProjectId!!
        val captured = navigation.launchDirect(projectId, AgentRuntime.Codex)
        assertNotNull(captured)
        assertEquals(projectId, captured.projectId)
        assertEquals(AgentRuntime.Codex, captured.runtime)
        assertNull(navigation.composingProjectId)
        assertEquals(captured.taskId, navigation.sidebar.selectedTaskId)
        assertEquals(1, repository.loadCatalog().projects.single().tasks.size)
    }

    private fun withRepository(block: (RoomProjectCatalogRepository, java.io.File) -> Unit) {
        val directory = Files.createTempDirectory("moru-navigation").toFile()
        val root = directory.resolve("project").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            block(RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                idGenerator = { java.util.UUID.randomUUID().toString() },
                nowEpochMillis = System::currentTimeMillis), root)
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
