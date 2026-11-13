package app.thdev.moru.app

import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity
import app.thdev.moru.core.data.*
import app.thdev.moru.core.domain.*
import app.thdev.moru.core.model.*
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.SessionLastLines
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.feature.terminal.releaseTerminalSession
import app.thdev.moru.feature.terminal.startDesktopTerminalSession
import app.thdev.moru.app.i18n.EnglishAppStrings
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.feature.codepanel.CodePanelSource
import app.thdev.moru.core.designsystem.theme.MoruPaneHeader
import java.io.File
import java.nio.file.Files
import java.awt.Frame
import java.awt.Component
import java.awt.Container
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.MouseEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.accessibility.AccessibleContext
import javax.swing.SwingUtilities
import kotlin.test.*
import kotlin.math.abs

/** The app + path uses real shell PTYs; providers are replaced by deterministic shell output. */
class AdditionalTaskSessionInteractionOsTest {
    @Test
    fun shortcutChooserAcceptsNumbersDeliveredToItsNativeOwner() = withTask { repository, folder, task, sessions ->
        val commands = MoruCommands()
        withApp(repository, folder, sessions, AppTheme.Dark, 1100, 760, commands = commands, appleStyle = true).use { ui ->
            ui.waitFor("initial terminal") { commands.newSession != null && ui.exists("New session") }
            pressNewSessionShortcut(commands)
            ui.waitFor("shortcut chooser") { ui.exists("What do you want to start with?") }
            // A native terminal can retain owner-window focus when the popup opens on macOS.
            SwingUtilities.invokeAndWait {
                val target = (sessionTestFrame() as javax.swing.RootPaneContainer).contentPane
                val focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                listOf(KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED).forEach { id ->
                    val modified = KeyEvent(target, id, System.currentTimeMillis(), InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_4, '4')
                    focus.dispatchEvent(modified)
                    assertFalse(modified.isConsumed, "Modified numbers remain outside menu shortcuts")
                }
                listOf(KeyEvent.KEY_PRESSED, KeyEvent.KEY_TYPED, KeyEvent.KEY_RELEASED).forEach { id ->
                    val code = if (id == KeyEvent.KEY_TYPED) KeyEvent.VK_UNDEFINED else KeyEvent.VK_4
                    val event = KeyEvent(target, id, System.currentTimeMillis(), 0, code, '4')
                    focus.dispatchEvent(event)
                    assertTrue(event.isConsumed, "Menu numbers never reach the underlying terminal")
                }
            }
            ui.waitFor("number starts a session and closes the chooser") {
                sessions.listSessions().size == 2 && !ui.exists("What do you want to start with?")
            }
            assertEquals(task.id, sessions.listSessions().last().spec.attachedItemId)
            SwingUtilities.invokeAndWait {
                val target = (sessionTestFrame() as javax.swing.RootPaneContainer).contentPane
                val event = KeyEvent(target, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_4, '4')
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().dispatchEvent(event)
                assertFalse(event.isConsumed, "Closing the chooser removes its native shortcut handler")
            }
        }
    }

    @Test
    fun resultSeamResizeReceivesNativePointerInputAndKeepsSessionControlsUsable() = withTask { repository, folder, _, sessions ->
        val source = object : CodePanelSource {
            override fun worktrees(projectFolder: String) = listOf(CodeWorktree(folder.path, "main", isMain = true))
            override fun sessionWorktreeRoots(sessionId: String) = listOf(folder.path)
            override fun changeStamp(worktreeRoot: String) = "unchanged"
            override fun changes(worktreeRoot: String) = CommitDiffResult.Loaded(emptyList(), truncated = false)
            override fun readFile(worktreeRoot: String, relativePath: String) = WorkbenchFilePreview.unavailable(relativePath, "No fixture file")
        }
        withApp(repository, folder, sessions, AppTheme.Dark, 1100, 760, appleStyle = true, codePanelSource = source).use { ui ->
            ui.waitFor("native Result resize target") { ui.exists("Resize Result pane") && ui.exists("New session") }
            fun resultLeft(): Int {
                var left = 0
                SwingUtilities.invokeAndWait {
                    fun descendants(component: Component): List<Component> = listOf(component) +
                        (component as? Container)?.components.orEmpty().flatMap(::descendants)
                    left = descendants(sessionTestFrame()).single { it.javaClass.simpleName == "ComposePanel" && it.isShowing }.locationOnScreen.x
                }
                return left
            }
            val originalIds = sessions.listSessions().map { it.id }
            val tabPadding = MoruPaneHeader.horizontalPadding.value.toInt()
            var before = 0
            ui.waitFor("native Result adjacent to the terminal tab padding") {
                before = resultLeft()
                val button = controlBounds("New session")
                abs(before - button.x - button.width - tabPadding) <= 1
            }
            val resize = controlBounds("Resize Result pane")
            assertEquals(8, resize.width)
            assertTrue(resize.height > 200, "The resize target must span the native panes")
            assertEquals(before, resize.x + resize.width / 2)
            mouseInput("Resize Result pane", dragX = -80)
            ui.waitFor("native Result width responds to seam drag") { resultLeft() <= before - 70 }
            val button = controlBounds("New session")
            assertTrue(abs(resultLeft() - button.x - button.width - tabPadding) <= 1, "Native bounds may round by one pixel")
            clickSessionButtonWithMouse()
            ui.waitFor("session chooser beside resized Result") { ui.exists("What do you want to start with?") }
            ui.pressEscapeInPopover()
            ui.waitFor("resized Result chooser cancelled") { !ui.exists("What do you want to start with?") }
            assertEquals(originalIds, sessions.listSessions().map { it.id })
        }
    }

    @Test
    fun sessionChooserReopensAfterCodeGitAndSettingsNavigation() = withTask { repository, folder, task, sessions ->
        val commands = MoruCommands()
        withApp(repository, folder, sessions, AppTheme.Dark, 1100, 760, commands = commands, appleStyle = true).use { ui ->
            ui.waitFor("initial Task terminal") { ui.selectedTab("Existing terminal") && commands.newSession != null }
            val originalIds = sessions.listSessions().map { it.id }
            listOf("Code" to { commands.showCode!!.invoke() }, "Git" to { commands.showGit!!.invoke() }).forEach { (name, visit) ->
                SwingUtilities.invokeAndWait(visit)
                ui.waitFor("$name project tab") { ui.selectedTab(name) && !ui.exists("New session") }
                SwingUtilities.invokeAndWait { commands.showWork!!.invoke() }
                ui.waitFor("Task terminal restored from $name") { ui.selectedTab("Existing terminal") && ui.exists("New session") }
                clickSessionButtonWithMouse()
                ui.waitFor("button chooser after $name") { ui.exists("What do you want to start with?") }
                ui.pressEscapeInPopover()
                ui.waitFor("button chooser cancelled after $name") { !ui.exists("What do you want to start with?") }
                pressNewSessionShortcut(commands)
                ui.waitFor("shortcut chooser after $name") { ui.exists("What do you want to start with?") }
                ui.pressEscapeInPopover()
                ui.waitFor("shortcut chooser cancelled after $name") { !ui.exists("What do you want to start with?") }

                // Leaving with the chooser open must not leave a stale popup or callback after returning.
                clickSessionButtonWithMouse()
                ui.waitFor("chooser before leaving for $name") { ui.exists("What do you want to start with?") }
                SwingUtilities.invokeAndWait(visit)
                ui.waitFor("$name before direct shortcut") { ui.selectedTab(name) && !ui.exists("New session") }
                pressNewSessionShortcut(commands)
                ui.waitFor("shortcut returns from $name and opens chooser") { ui.exists("What do you want to start with?") }
                ui.pressEscapeInPopover()
                ui.waitFor("direct shortcut cancelled after $name") { !ui.exists("What do you want to start with?") }
            }
            SwingUtilities.invokeAndWait { commands.showSettings!!.invoke() }
            ui.waitFor("Settings replaces the Workbench") { !ui.exists("New session") }
            pressNewSessionShortcut(commands)
            ui.waitFor("shortcut returns from Settings and opens chooser") { ui.exists("What do you want to start with?") }
            ui.pressEscapeInPopover()
            ui.waitFor("Settings shortcut cancelled") { !ui.exists("What do you want to start with?") }
            clickSessionButtonWithMouse()
            ui.waitFor("button chooser after Settings") { ui.exists("What do you want to start with?") }
            ui.click("Terminal")
            ui.waitFor("one additional session after navigation") { sessions.listSessions().size == 2 && !ui.exists("What do you want to start with?") }
            assertEquals(originalIds, sessions.listSessions().take(1).map { it.id })
            assertEquals(task.id, sessions.listSessions().last().spec.attachedItemId)
            assertEquals(task.id, repository.loadCatalog().selectedTaskId)
        }
    }

    @Test
    fun newlyCreatedTaskKeepsSessionActionAndChooserAcrossTaskSwitches() = withTask { repository, folder, task, sessions ->
        val commands = MoruCommands()
        withApp(repository, folder, sessions, AppTheme.Dark, 800, 700, commands = commands, appleStyle = true).use { ui ->
            ui.waitFor("New Task command") { commands.newTask != null }
            SwingUtilities.invokeAndWait { commands.newTask!!.invoke() }
            ui.waitFor("new Task composer") { ui.exists("What do you want to start with?") }
            ui.click("Terminal")
            ui.waitFor("new Task terminal") {
                repository.loadCatalog().projects.single().tasks.size == 2 && ui.selectedTab("Terminal")
            }
            val createdTaskId = repository.loadCatalog().selectedTaskId!!
            assertNotEquals(task.id, createdTaskId)
            ui.waitFor("named session button in the new Task") { ui.exists("New session") }
            clickSessionButtonWithMouse()
            ui.waitFor("new Task session chooser") { ui.exists("What do you want to start with?") }
            ui.pressEscapeInPopover()
            ui.waitFor("new Task chooser cancelled") { !ui.exists("What do you want to start with?") }

            ui.click("Task Existing task")
            ui.waitFor("original Task terminal") { ui.selectedTab("Existing terminal") }
            SwingUtilities.invokeAndWait { commands.nextTask!!.invoke() }
            ui.waitFor("new Task restored") {
                repository.loadCatalog().selectedTaskId == createdTaskId && ui.selectedTab("Terminal")
            }
            pressNewSessionShortcut(commands)
            ui.waitFor("restored Task session chooser") { ui.exists("What do you want to start with?") }
            ui.click("Terminal")
            ui.waitFor("another session in the new Task") {
                sessions.listSessions().count { it.spec.attachedItemId == createdTaskId } == 2 &&
                    !ui.exists("What do you want to start with?")
            }
            assertEquals(1, sessions.listSessions().count { it.spec.attachedItemId == task.id })
            assertEquals(2, repository.loadCatalog().projects.single().tasks.size)
        }
    }

    private fun sessionTestFrame(): Frame = Window.getWindows().filterIsInstance<Frame>()
        .single { it.isShowing && it.title == "Task session test" }

    // Use the production shortcut dispatcher with this window's live command bindings; global OS focus can
    // belong to another app, and macOS denies Robot key posting in this test runtime.
    private fun pressNewSessionShortcut(commands: MoruCommands, enabled: Boolean = true) = SwingUtilities.invokeAndWait {
        val frame = sessionTestFrame()
        val dispatcher = AppShortcutDispatcher({ appMenu(EnglishAppStrings, commands).flatMap { it.entries } },
            macOs = true, belongsToWindow = { it == frame })
        listOf(KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED).forEach { id ->
            val event = KeyEvent(frame, id, System.currentTimeMillis(), InputEvent.META_DOWN_MASK,
                KeyEvent.VK_T, KeyEvent.CHAR_UNDEFINED)
            assertEquals(enabled, dispatcher.dispatchKeyEvent(event), "Cmd+T follows live session admission")
            assertEquals(enabled, event.isConsumed, "Only an enabled command intercepts terminal input")
        }
    }

    // Exercise the pointer hit target, rather than its accessibility action. Posting OS input requires
    // macOS permissions and can click another app; AWT delivery keeps the test scoped to its own window.
    private fun clickSessionButtonWithMouse() = mouseInput("New session")

    private fun findControl(node: AccessibleContext, text: String): AccessibleContext? {
        if (node.accessibleName == text || node.accessibleDescription == text) return node
        return (0 until node.accessibleChildrenCount).firstNotNullOfOrNull { index ->
            node.getAccessibleChild(index)?.accessibleContext?.let { findControl(it, text) }
        }
    }

    private fun controlBounds(text: String): Rectangle {
        var bounds: Rectangle? = null
        SwingUtilities.invokeAndWait {
            val control = requireNotNull(findControl(sessionTestFrame().accessibleContext, text)).accessibleComponent
            bounds = Rectangle(control.locationOnScreen, control.size)
        }
        return bounds!!
    }

    private fun mouseInput(text: String, dragX: Int = 0) {
        SwingUtilities.invokeAndWait {
            val frame = sessionTestFrame()
            val control = requireNotNull(findControl(frame.accessibleContext, text)).accessibleComponent
            val origin = control.locationOnScreen
            val screen = Point(origin.x + control.size.width / 2, origin.y + control.size.height / 2)
            val local = Point(screen)
            SwingUtilities.convertPointFromScreen(local, frame)
            val target = requireNotNull(SwingUtilities.getDeepestComponentAt(frame, local.x, local.y))
            val relative = SwingUtilities.convertPoint(frame, local, target)
            val events = if (dragX == 0) listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)
                else listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_DRAGGED, MouseEvent.MOUSE_RELEASED)
            events.forEach { id ->
                val delta = if (id == MouseEvent.MOUSE_PRESSED) 0 else dragX
                target.dispatchEvent(MouseEvent(target, id, System.currentTimeMillis(),
                    if (id != MouseEvent.MOUSE_RELEASED) InputEvent.BUTTON1_DOWN_MASK else 0,
                    relative.x + delta, relative.y, screen.x + delta, screen.y, 1, false,
                    if (id == MouseEvent.MOUSE_DRAGGED) MouseEvent.NOBUTTON else MouseEvent.BUTTON1))
            }
        }
    }

    @Test
    fun aFullTaskDisablesNewSessionAcrossNavigationAndClosingReenablesIt() = withTask { repository, folder, task, sessions ->
        // The Task starts with one terminal; these fill it to one short of the maximum.
        repeat(TaskTerminalSessionCapacity.Maximum - 2) { index ->
            sessions.createSession(TerminalSessionSpec(title = "Existing ${index + 2}", shell = "/bin/sh",
                workingDirectory = folder.canonicalPath, attachedItemId = task.id))
        }
        val originalIds = sessions.listSessions().map { it.id }
        val commands = MoruCommands()
        var starts = 0
        withApp(repository, folder, sessions, AppTheme.Light, 1100, 760, beforeStart = { starts++ }, commands = commands).use { ui ->
            ui.waitFor("one short of the maximum admits one more session") { commands.newSession != null && ui.exists("New session") }
            val staleCommand = commands.newSession!!
            clickSessionButtonWithMouse()
            ui.waitFor("last-session chooser") { ui.exists("What do you want to start with?") }
            ui.click("Terminal")
            ui.waitFor("last session created") { sessions.listSessions().size == TaskTerminalSessionCapacity.Maximum }
            ui.waitFor("last terminal tab selected") { ui.selectedTab("Terminal") }
            ui.waitFor("new-session command disabled at the maximum") { commands.newSession == null }
            val added = sessions.listSessions().last()
            assertEquals(task.id, added.spec.attachedItemId)
            assertEquals(folder.canonicalPath, added.spec.workingDirectory)
            assertEquals(added.id, sessions.selectedSessionId())
            assertEquals(originalIds, sessions.listSessions().take(originalIds.size).map { it.id })
            assertEquals(1, repository.loadCatalog().projects.single().tasks.size)
            val atCapacity = sessions.listSessions().map { it.id }
            clickSessionButtonWithMouse()
            SwingUtilities.invokeAndWait { staleCommand() }
            pressNewSessionShortcut(commands, enabled = false)
            ui.capture("additional-session-four-capacity")
            assertFalse(ui.exists("What do you want to start with?"))
            assertEquals(atCapacity, sessions.listSessions().map { it.id })
            assertEquals(added.id, sessions.selectedSessionId())
            assertEquals(1, starts, "No PTY past the maximum may be started")
            assertFalse(appMenu(EnglishAppStrings, commands).flatMap { it.entries }
                .single { it.label == EnglishAppStrings.menuNewSession }.enabled)

            listOf("Code" to { commands.showCode!!.invoke() }, "Git" to { commands.showGit!!.invoke() }).forEach { (name, visit) ->
                SwingUtilities.invokeAndWait(visit)
                ui.waitFor("$name at capacity") { ui.selectedTab(name) && commands.newSession == null }
                SwingUtilities.invokeAndWait { staleCommand() }
                pressNewSessionShortcut(commands, enabled = false)
                assertTrue(ui.selectedTab(name), "A retained callback must not navigate or open a fifth-session chooser")
                SwingUtilities.invokeAndWait { commands.showWork!!.invoke() }
                ui.waitFor("disabled session action restored from $name") { ui.exists("New session") && commands.newSession == null }
            }
            SwingUtilities.invokeAndWait { commands.showSettings!!.invoke() }
            ui.waitFor("Settings at capacity") { !ui.exists("New session") && commands.newSession == null }
            pressNewSessionShortcut(commands, enabled = false)
            SwingUtilities.invokeAndWait { staleCommand() }
            assertFalse(ui.exists("New session"), "A disabled command must leave Settings in view")
            SwingUtilities.invokeAndWait { commands.showWork!!.invoke() }
            ui.waitFor("disabled action restored from Settings") { ui.exists("New session") && commands.newSession == null }

            SwingUtilities.invokeAndWait { commands.newTask!!.invoke() }
            ui.waitFor("another Task composer despite the first Task being full") { ui.exists("What do you want to start with?") }
            ui.click("Terminal")
            ui.waitFor("another Task has independent admission") {
                repository.loadCatalog().selectedTaskId != task.id && commands.newSession != null && ui.exists("New session")
            }
            assertEquals(atCapacity, sessions.listSessions().filter { it.spec.attachedItemId == task.id }.map { it.id })
            ui.click("Task Existing task")
            ui.waitFor("original full Task retains its capacity") { commands.newSession == null && ui.exists("New session") }
            SwingUtilities.invokeAndWait { sessions.closeSession(added.id); releaseTerminalSession(added.id) }
            ui.waitFor("closing a terminal re-enables button and command") { commands.newSession != null && ui.exists("New session") }
            pressNewSessionShortcut(commands)
            ui.waitFor("shortcut chooser after closing") { ui.exists("What do you want to start with?") }
            ui.click("Terminal")
            ui.waitFor("one replacement fills the Task again") {
                sessions.listSessions().count { it.spec.attachedItemId == task.id } == TaskTerminalSessionCapacity.Maximum && commands.newSession == null
            }
            assertEquals(originalIds, sessions.listSessions().filter { it.spec.attachedItemId == task.id }.take(originalIds.size).map { it.id })
        }
    }

    @Test
    fun plusChoosesEachAgentInTheSameTaskAndCancelKeepsExistingExecution() = withTask { repository, folder, task, sessions ->
        withApp(repository, folder, sessions, AppTheme.Light, 1100, 760).use { ui ->
            val existingId = sessions.listSessions().single().id
            ui.click("New session")
            ui.waitFor("agent picker") { ui.exists("What do you want to start with?") }
            ui.capture("additional-session-picker-light")
            assertEquals(listOf(existingId), sessions.listSessions().map { it.id })
            // The picker is a popover now: Escape closes it the way an outside click does.
            ui.pressEscapeInPopover()
            ui.waitFor("picker dismissed") { !ui.exists("What do you want to start with?") }
            assertEquals(existingId, sessions.selectedSessionId())
            ui.click("Existing terminal")
            listOf("Claude", "Codex", "AGY").forEachIndexed { index, runtime ->
                ui.click("New session")
                ui.waitFor("agent picker") { ui.exists("What do you want to start with?") }
                ui.click(runtime)
                ui.waitFor("$runtime selected and picker dismissed") {
                    !ui.exists("What do you want to start with?") && ui.selectedTab(runtime)
                }
                val added = sessions.listSessions().last()
                assertEquals(index + 2, sessions.listSessions().size)
                assertEquals(runtime, added.title)
                assertEquals(task.id, added.spec.attachedItemId)
                assertEquals(folder.canonicalPath, added.spec.workingDirectory)
                assertEquals(added.id, sessions.selectedSessionId())
                assertEquals(1, repository.loadCatalog().projects.single().tasks.size)
                assertEquals(task.id, repository.loadCatalog().selectedTaskId)
                ui.click("Existing terminal")
                sessions.selectSession(added.id)
                ui.waitFor("external session selection reaches terminal tabs") { ui.selectedTab(runtime) }
            }
            ui.waitFor("live PTY work line") {
                SessionLastLines.lineFor(sessions.listSessions().last().id)?.contains("Session work visible") == true
            }
            // A session row says what the session is doing in words, not by echoing its raw output.
            assertFalse(ui.exists("Session work visible", substring = true))
            ui.capture("additional-session-work-content")
        }
    }

    @Test
    fun failedLaunchKeepsNarrowDarkPickerOpenAndRetryAddsOnlyOneTerminal() = withTask { repository, folder, task, sessions ->
        var failNext = true
        withApp(repository, folder, sessions, AppTheme.Dark, 800, 700, beforeStart = {
            if (failNext) { failNext = false; error("Test shell startup failed") }
        }).use { ui ->
            val original = sessions.listSessions().single()
            ui.click("New session")
            ui.waitFor("agent picker") { ui.exists("What do you want to start with?") }
            ui.click("Codex")
            ui.waitFor("startup error") { ui.exists("Test shell startup failed", substring = true) }
            assertTrue(ui.exists("What do you want to start with?"))
            assertEquals(listOf(original.id), sessions.listSessions().map { it.id })
            assertEquals(original.spec, sessions.listSessions().single().spec)
            assertEquals(original.id, sessions.selectedSessionId())
            ui.capture("additional-session-picker-error-narrow-dark")
            ui.click("Terminal")
            ui.waitFor("retry selects terminal") { !ui.exists("What do you want to start with?") && ui.selectedTab("Terminal") }
            assertEquals(2, sessions.listSessions().size)
            assertEquals(task.id, sessions.listSessions().last().spec.attachedItemId)
            assertEquals(1, repository.loadCatalog().projects.single().tasks.size)
        }
    }

    private fun withApp(
        repository: RoomProjectCatalogRepository, folder: File, sessions: InMemoryTerminalSessionManager,
        theme: AppTheme, width: Int, height: Int, beforeStart: () -> Unit = {},
        commands: MoruCommands = MoruCommands(),
        appleStyle: Boolean = false,
        codePanelSource: CodePanelSource? = null,
    ): NativeWorkbenchTestWindow {
        val settings = object : SettingsRepository {
            private var value = AppSettings(appearance = AppearanceSettings(theme = theme, appleStyle = appleStyle), terminal = TerminalSettings(shellCommand = "/bin/sh"))
            override fun loadSettings() = value
            override fun saveSettings(settings: AppSettings) { value = settings }
        }
        val workspace = object : TerminalWorkspaceRepository {
            override fun loadWorkspaceState() = TerminalWorkspaceState()
            override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
        }
        return NativeWorkbenchTestWindow(width, height, menu = { MoruCommandMenuBar(EnglishAppStrings, commands) }) {
            MoruApp(
                folder.path, sessions, StaticRuntimeStatusRepository, settings, workspace, repository,
                commands = commands,
                codePanelSource = codePanelSource,
                onStartTerminalSession = { session, _, colors ->
                    beforeStart()
                    startDesktopTerminalSession(
                        sessionKey = session.id, workingDirectory = session.spec.workingDirectory, shell = "/bin/sh",
                        startupCommand = "PS1=''; printf 'Session work visible\\n'",
                        onCommandCaptured = { sessions.recordCommandHistory(session.id, it) },
                        fontFamily = null, fontSize = 13, scrollbackLines = 100,
                        onOutput = { sessions.reportOutput(session.id, it) }, onExit = { sessions.reportExit(session.id, it) }, colors = colors,
                    )
                },
            )
        }
    }

    private fun withTask(block: (RoomProjectCatalogRepository, File, Task, InMemoryTerminalSessionManager) -> Unit) {
        val directory = Files.createTempDirectory("moru-session-ui").toFile()
        val folder = directory.resolve("Project folder").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        val sessions = InMemoryTerminalSessionManager()
        try {
            val repository = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis)
            val project = repository.registerProject("", folder.path).project
            val task = repository.createTask(project.id, "Existing task", "")
            repository.updateSelection(project.id, task.id)
            sessions.createSession(TerminalSessionSpec(title = "Existing terminal", shell = "/bin/sh",
                workingDirectory = folder.canonicalPath, attachedItemId = task.id))
            block(repository, folder, task, sessions)
        } finally {
            sessions.listSessions().forEach { releaseTerminalSession(it.id) }
            database.close()
            directory.deleteRecursively()
        }
    }
}
