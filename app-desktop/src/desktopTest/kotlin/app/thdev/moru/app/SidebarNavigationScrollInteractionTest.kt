package app.thdev.moru.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.EnglishAppStrings
import app.thdev.moru.core.data.DesktopProjectRootCanonicalizer
import app.thdev.moru.core.data.RoomProjectCatalogRepository
import app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.model.KeyboardSettings
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.sessions.rememberAgentSessionRows
import app.thdev.moru.feature.workbench.ProjectTaskSidebar
import java.awt.Canvas
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** App shortcut dispatch and catalog/session selection drive the real sidebar and session row views. */
@OptIn(ExperimentalTestApi::class)
class SidebarNavigationScrollInteractionTest {
    @Test
    fun aProjectNumberShortcutRevealsItsHeaderAboveTheRememberedTask() = withCatalog { fixture ->
        fixture.project("First project")
        val target = fixture.project("Target project")
        val tasks = List(12) { fixture.repository.createTask(target, "Task $it", "") }
        fixture.repository.updateSelection(target, tasks.last().id)
        fixture.selectFirst()
        runDesktopComposeUiTest(width = 500, height = 440) {
            showSidebar(fixture)
            press(fixture, AppShortcut.openProjectByPosition[1])
            assertFullyVisible(onNodeWithContentDescription("Project Target project"))
            val header = onNodeWithContentDescription("Project Target project").getUnclippedBoundsInRoot()
            val viewport = onNodeWithTag("sidebar-viewport").getUnclippedBoundsInRoot()
            assertTrue(header.top >= viewport.top + 16.dp && header.top <= viewport.top + 40.dp,
                "Project header $header must have breathing room near the top of $viewport")
            assertEquals(tasks.last().id, fixture.navigation.sidebar.selectedTaskId)
            // Repeating the same shortcut after manual scrolling must also reveal the Project again.
            onNodeWithContentDescription("Task Task 11").performScrollTo()
            press(fixture, AppShortcut.openProjectByPosition[1])
            assertFullyVisible(onNodeWithContentDescription("Project Target project"))
            press(fixture, AppShortcut.PreviousTask)
            assertEquals(tasks[10].id, fixture.navigation.sidebar.selectedTaskId)
            assertFullyVisible(onNodeWithContentDescription("Task Task 10"))
        }
    }

    @Test
    fun newTaskShortcutRevealsTheCurrentProjectAndOpensItsChooserBesideTheHeader() = withCatalog { fixture ->
        fixture.project("First project")
        val project = fixture.project("Target project")
        val tasks = List(12) { fixture.repository.createTask(project, "Task $it", "") }
        fixture.repository.updateSelection(project, tasks.last().id)
        runDesktopComposeUiTest(width = 500, height = 440) {
            showSidebar(fixture)
            onNodeWithContentDescription("Task Task 11").performScrollTo()
            assertFullyVisible(onNodeWithContentDescription("Task Task 11"))
            repeat(2) {
                press(fixture, AppShortcut.NewTask)
                assertFullyVisible(onNodeWithContentDescription("Project Target project"))
                val header = onNodeWithContentDescription("Project Target project").getUnclippedBoundsInRoot()
                val chooser = onNodeWithText("Claude").getUnclippedBoundsInRoot()
                val viewport = onNodeWithTag("sidebar-viewport").getUnclippedBoundsInRoot()
                assertTrue(header.top >= viewport.top + 16.dp && header.top <= viewport.top + 40.dp)
                assertTrue(chooser.top > header.bottom && chooser.top < header.bottom + 100.dp,
                    "Chooser $chooser must open beside the Project header $header")
                onNodeWithText("Claude").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Escape) }
                onNodeWithText("Claude").assertDoesNotExist()
                onNodeWithContentDescription("Task Task 11").performScrollTo()
            }
            press(fixture, AppShortcut.NewTask)
            onNodeWithText("Claude").performClick()
            assertEquals(listOf(project), fixture.startedProjects)
        }
    }

    @Test
    fun projectShortcutsRevealOffscreenHeadersInBothDirectionsAndOnWrap() = withCatalog { fixture ->
        repeat(14) { fixture.project("Project $it") }
        fixture.selectFirst()
        runDesktopComposeUiTest(width = 500, height = 440) {
            showSidebar(fixture)
            assertFullyVisible(onNodeWithContentDescription("Project Project 0"))
            press(fixture, AppShortcut.PreviousProject)
            assertFullyVisible(onNodeWithContentDescription("Project Project 13"))
            press(fixture, AppShortcut.NextProject)
            assertFullyVisible(onNodeWithContentDescription("Project Project 0"))
            repeat(13) { press(fixture, AppShortcut.NextProject) }
            assertFullyVisible(onNodeWithContentDescription("Project Project 13"))
        }
    }

    @Test
    fun sessionShortcutsRevealTheSessionInsideATaskTallerThanTheViewport() = withCatalog { fixture ->
        val project = fixture.project("Project")
        val task = fixture.repository.createTask(project, "Long task", "")
        repeat(12) { fixture.session(task.id, "Session $it") }
        fixture.repository.updateSelection(project, task.id)
        fixture.selectFirstSession()
        runDesktopComposeUiTest(width = 500, height = 440) {
            showSidebar(fixture)
            assertFullyVisible(onNodeWithText("Session 0"))
            press(fixture, AppShortcut.PreviousSession)
            assertFullyVisible(onNodeWithText("Session 11"))
            assertEquals(fixture.sessions.listSessions().last().id, fixture.sessions.selectedSessionId())
            press(fixture, AppShortcut.NextSession)
            assertFullyVisible(onNodeWithText("Session 0"))
            // A partly visible next row must also be revealed, not only rows wholly outside the viewport.
            repeat(11) { press(fixture, AppShortcut.NextSession) }
            assertFullyVisible(onNodeWithText("Session 11"))
        }
    }

    @Test
    fun taskShortcutsRevealTheTaskWithoutTryingToFitItsWholeSessionGroup() = withCatalog { fixture ->
        val project = fixture.project("Project")
        val first = fixture.repository.createTask(project, "First task", "")
        repeat(10) { fixture.session(first.id, "Session $it") }
        fixture.repository.createTask(project, "Last task", "")
        fixture.repository.updateSelection(project, first.id)
        fixture.selectFirstSession()
        runDesktopComposeUiTest(width = 500, height = 440) {
            showSidebar(fixture)
            press(fixture, AppShortcut.NextTask)
            assertFullyVisible(onNodeWithContentDescription("Task Last task"))
            press(fixture, AppShortcut.PreviousTask)
            assertFullyVisible(onNodeWithText("Session 0"))
        }
    }

    @Test
    fun backgroundRowRefreshPreservesManualScrollAwayFromTheSelectedSession() = withCatalog { fixture ->
        val project = fixture.project("Project")
        val task = fixture.repository.createTask(project, "Long task", "")
        repeat(12) { fixture.session(task.id, "Session $it") }
        fixture.repository.updateSelection(project, task.id)
        fixture.selectFirstSession()
        runDesktopComposeUiTest(width = 500, height = 440) {
            showSidebar(fixture)
            assertFullyVisible(onNodeWithText("Session 0"))
            onNodeWithText("Session 11").performScrollTo()
            waitForIdle()
            val before = onNodeWithText("Session 11").getUnclippedBoundsInRoot()
            runOnIdle { fixture.revision++ }
            waitForIdle()
            assertEquals(before, onNodeWithText("Session 11").getUnclippedBoundsInRoot())
        }
    }

    private fun ComposeUiTest.showSidebar(fixture: Fixture) {
        setContent {
            val navigation = fixture.navigation
            KeepAppCommands(fixture.commands, navigation, KeyboardSettings()) {}
            SideEffect {
                fun move(step: Int) {
                    adjacentTaskSession(fixture.sessions, navigation.sidebar.selectedTaskId, step)?.let {
                        fixture.sessions.selectSession(it)
                        fixture.revision++
                    }
                }
                fixture.commands.nextSession = { move(1) }
                fixture.commands.previousSession = { move(-1) }
                fixture.commands.newTask = { navigation.newTaskRequest = navigation.sidebar.selectedProjectId }
            }
            val rows = rememberAgentSessionRows(fixture.sessions, fixture.revision.toLong(), scans = false)
            MoruTheme {
                ProjectTaskSidebar(
                    state = navigation.sidebar,
                    onOpenProject = {},
                    onSelectProject = navigation::selectProject,
                    onToggleProject = navigation::toggleProject,
                    onSelectTask = navigation::selectTask,
                    onNewTask = { fixture.startedProjects += it },
                    newTaskRequestedProjectId = navigation.newTaskRequest,
                    onNewTaskRequestHandled = { navigation.newTaskRequest = null },
                    projectRevealRequest = navigation.projectRevealRequest,
                    onProjectRevealHandled = { navigation.projectRevealRequest = null },
                    modifier = Modifier.width(320.dp).height(400.dp),
                    sessionSlots = DesktopWorkbenchSessionSlots,
                    sessions = rows,
                    footer = { Box(Modifier.height(80.dp)) },
                    showHeader = false,
                )
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.press(fixture: Fixture, shortcut: AppShortcut) {
        runOnIdle {
            val chord = fixture.commands.keyboard.chordFor(shortcut)!!
            val key = app.thdev.moru.core.ui.shortcut.ShortcutKeys.key(chord.key)!!.nativeKeyCode
            val modifiers = InputEvent.META_DOWN_MASK or
                (if (chord.alt) InputEvent.ALT_DOWN_MASK else 0) or
                (if (chord.shift) InputEvent.SHIFT_DOWN_MASK else 0)
            val dispatcher = AppShortcutDispatcher({ appMenu(EnglishAppStrings, fixture.commands).flatMap { it.entries } }, true) { true }
            val target = Canvas()
            assertTrue(dispatcher.dispatchKeyEvent(KeyEvent(target, KeyEvent.KEY_PRESSED, 0, modifiers, key, KeyEvent.CHAR_UNDEFINED)))
            dispatcher.dispatchKeyEvent(KeyEvent(target, KeyEvent.KEY_RELEASED, 0, modifiers, key, KeyEvent.CHAR_UNDEFINED))
        }
        waitForIdle()
    }

    private fun ComposeUiTest.assertFullyVisible(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val row = node.getUnclippedBoundsInRoot()
        val viewport = onNodeWithTag("sidebar-viewport").getUnclippedBoundsInRoot()
        assertTrue(row.top >= viewport.top - 1.dp && row.bottom <= viewport.bottom + 1.dp,
            "Selected row $row must fit inside sidebar viewport $viewport")
    }

    private fun withCatalog(block: (Fixture) -> Unit) {
        val directory = Files.createTempDirectory("moru-sidebar-scroll").toFile()
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            var clock = 1_000L
            val repository = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, { clock++ })
            block(Fixture(repository, directory))
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    private class Fixture(val repository: RoomProjectCatalogRepository, val directory: java.io.File) {
        val sessions = InMemoryTerminalSessionManager()
        val commands = MoruCommands()
        val startedProjects = mutableListOf<String>()
        val navigation by lazy { WorkbenchNavigation(repository) }
        var revision by mutableIntStateOf(0)
        fun project(name: String): String = repository.registerProject("", directory.resolve(name).apply { mkdir() }.path).project.id
        fun session(task: String, title: String) = sessions.createSession(TerminalSessionSpec(title = title, attachedItemId = task))
        fun selectFirst() = repository.updateSelection(repository.loadCatalog().projects.first().project.id, null)
        fun selectFirstSession() = sessions.selectSession(sessions.listSessions().first().id)
    }
}
