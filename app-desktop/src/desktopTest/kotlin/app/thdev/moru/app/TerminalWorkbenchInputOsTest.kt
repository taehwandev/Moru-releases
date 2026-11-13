package app.thdev.moru.app

import app.thdev.moru.core.data.*
import app.thdev.moru.core.domain.*
import app.thdev.moru.core.model.*
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.app.i18n.EnglishAppStrings
import app.thdev.moru.feature.terminal.releaseTerminalSession
import java.awt.Component
import java.awt.Container
import java.awt.Desktop
import java.awt.KeyboardFocusManager
import java.awt.Robot
import java.awt.Toolkit
import java.awt.Window
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseMotionAdapter
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.swing.BoundedRangeModel
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Physical input through the full Workbench and its real terminal session. */
class TerminalWorkbenchInputOsTest {
    @Test
    fun commandWAsksAndCommandWAgainClosesTheTerminalInView() = withWorkbench { ui, input, sessions ->
        val robot = Robot().apply { autoDelay = 50 }
        focusTerminal(ui, input, robot)
        val before = input.readText()
        val closing = sessions.listSessions().single().id
        shortcut(robot, KeyEvent.VK_W)
        ui.waitFor("Command+W to ask before closing the terminal") { ui.exists("Press ⌘W again to close.") }
        assertEquals(before, input.readText(), "Command+W must not type into the terminal")
        assertEquals(1, sessions.listSessions().size, "Asking must not close anything yet")
        shortcut(robot, KeyEvent.VK_W)
        ui.waitFor("Command+W again to close the terminal") { sessions.listSessions().none { it.id == closing } }
        ui.waitFor("the question to go away") { !ui.exists("Close this terminal?", substring = true) }
    }

    @Test
    fun commandASelectsTheEntireTerminalHistoryAndCopiesWithoutTyping() = withWorkbench { ui, input, _ ->
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val previous = clipboard.getContents(null)
        val robot = Robot().apply { autoDelay = 50 }
        try {
            clipboard.setContents(StringSelection("nothing-copied"), null)
            val panel = focusTerminal(ui, input, robot)
            val before = input.readText()
            shortcut(robot, KeyEvent.VK_A)
            ui.waitFor("Command+A to select the terminal history") {
                onEdt { panel.javaClass.getMethod("getSelection").invoke(panel) != null }
            }
            shortcut(robot, KeyEvent.VK_C)
            val records = (1..80).map { "record-%02d".format(it) }
            ui.waitFor("Command+A then Command+C to copy all output, including offscreen history") {
                clipboard.getData(DataFlavor.stringFlavor).toString().lines().filter { it.startsWith("record-") } == records
            }
            assertEquals(before, input.readText(), "Select all and copy must not type into the CLI")
        } finally {
            if (previous != null) clipboard.setContents(previous, null)
        }
    }

    @Test
    fun draggingJustBeyondTheTopEdgeKeepsScrollingAndCopiesTheExpandedHistory() = withWorkbench { ui, input, _ ->
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val previous = clipboard.getContents(null)
        val robot = Robot().apply { autoDelay = 50 }
        var held = false
        try {
            clipboard.setContents(StringSelection("nothing-copied"), null)
            val panel = focusTerminal(ui, input, robot)
            val before = input.readText()
            val scroll = panel.javaClass.getMethod("getVerticalScrollModel").invoke(panel) as BoundedRangeModel
            val releases = AtomicInteger()
            onEdt { panel.addMouseListener(object : MouseAdapter() {
                override fun mouseReleased(event: MouseEvent) { releases.incrementAndGet() }
            }) }
            val buffer = panel.javaClass.getMethod("getTerminalTextBuffer").invoke(panel)
            val rows = number(buffer, "getHeight")
            val cellHeight = number(panel, "getPixelHeight") / rows
            val cellWidth = (number(panel, "getPixelWidth") - 4) / number(buffer, "getWidth")
            val row = rows - 3
            val startRecord = onEdt { recordAt(buffer, row) }
            val origin = onEdt { panel.locationOnScreen }
            val startY = origin.y + row * cellHeight + cellHeight / 2
            robot.mouseMove(origin.x + 4 + 9 * cellWidth, startY)
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            held = true
            for (step in 1..12) robot.mouseMove(origin.x + 4, startY + (origin.y - 6 - startY) * step / 12)
            // The pointer stays still just outside the pane: no wheel events and no further mouse motion.
            ui.waitFor("the held edge drag to reach the oldest history without moving the pointer") {
                onEdt { scroll.value == scroll.minimum }
            }
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            held = false
            robot.waitForIdle()
            assertTrue(onEdt { panel.isFocusOwner }, "An outside-edge drag must keep terminal keyboard focus")
            shortcut(robot, KeyEvent.VK_C)
            ui.waitFor("edge copy to update the native clipboard") { clipboard.getData(DataFlavor.stringFlavor) != "nothing-copied" }
            val expected = (1..startRecord).map { "record-%02d".format(it) }
            assertEquals(expected, clipboard.getData(DataFlavor.stringFlavor).toString().lines().filter { it.startsWith("record-") },
                "Copy must include every record reached by edge scrolling, including offscreen history")
            assertEquals(before, input.readText(), "Edge selection must remain local to the terminal")
            robot.mouseMove(origin.x + 4, origin.y + cellHeight / 2)
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            held = true
            robot.mouseMove(origin.x + 4, origin.y + panel.height + 6)
            ui.waitFor("a bottom-edge drag to scroll from the shell prelude into newer records") {
                onEdt { scroll.value > scroll.minimum && lineText(buffer, scroll.value).trim().startsWith("record-") }
            }
            val releaseCount = releases.get()
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            held = false
            ui.waitFor("the native mouse release") { releases.get() > releaseCount }
            val releasedAt = onEdt { scroll.value }
            assertTrue(releasedAt < 0, "The release check must happen before reaching the bottom boundary")
            Thread.sleep(250)
            assertEquals(releasedAt, onEdt { scroll.value }, "Releasing the mouse must stop automatic scrolling")
            val firstRemainingRecord = onEdt { recordAt(buffer, scroll.value) }
            robot.mouseMove(origin.x + 4, origin.y + cellHeight / 2)
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            held = true
            robot.mouseMove(origin.x + 4, origin.y + panel.height + 6)
            ui.waitFor("the stationary bottom-edge drag to reach newer output") { onEdt { scroll.value == 0 } }
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            held = false
            robot.waitForIdle()
            shortcut(robot, KeyEvent.VK_C)
            val remaining = (firstRemainingRecord..80).map { "record-%02d".format(it) }
            ui.waitFor("copy to include the range reached at the bottom edge") {
                clipboard.getData(DataFlavor.stringFlavor).toString().lines().filter { it.startsWith("record-") } == remaining
            }
            assertEquals(before, input.readText(), "Both edge-drag directions must remain local")
        } finally {
            if (held) robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            if (previous != null) clipboard.setContents(previous, null)
        }
    }

    @Test
    fun realWorkbenchClickDragScrollCopyPasteAndShiftEnterReachTheTerminal() = withWorkbench { ui, input, sessions ->
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val previous = clipboard.getContents(null)
        clipboard.setContents(StringSelection("nothing-copied"), null)
        val robot = Robot().apply { autoDelay = 50 }
        var held = false
        try {
            var terminal: Component? = null
            ui.waitFor("the actual Workbench terminal") {
                terminal = onEdt { Window.getWindows().filter { it.isShowing }.flatMap(::descendants)
                    .firstOrNull { it.javaClass.simpleName == "MoruTerminalPanel" && it.isShowing } }
                terminal != null && input.exists()
            }
            val panel = requireNotNull(terminal)
            val scroll = panel.javaClass.getMethod("getVerticalScrollModel").invoke(panel) as BoundedRangeModel
            ui.waitFor("history output") { onEdt { scroll.minimum < 0 } }
            val buffer = panel.javaClass.getMethod("getTerminalTextBuffer").invoke(panel)
            val rows = number(buffer, "getHeight")
            val cellHeight = number(panel, "getPixelHeight") / rows
            val cellWidth = (number(panel, "getPixelWidth") - 4) / number(buffer, "getWidth")
            val row = rows - 3
            val startRecord = onEdt { recordAt(buffer, row) }
            val origin = onEdt { panel.locationOnScreen }
            val y = origin.y + row * cellHeight + cellHeight / 2
            Desktop.getDesktop().requestForeground(true)
            onEdt { SwingUtilities.getWindowAncestor(panel).apply { toFront(); requestFocus() } }
            robot.mouseMove(origin.x + 4 + 9 * cellWidth, y)
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            robot.waitForIdle()
            ui.waitFor("a native click to give the terminal keyboard focus") { onEdt { panel.isFocusOwner } }
            ui.waitFor("the ordinary click to reach the mouse-reporting process") { input.length() > 0 }
            val before = input.readText()
            val dragX = AtomicInteger(Int.MAX_VALUE)
            val rotation = AtomicInteger()
            onEdt {
                panel.addMouseMotionListener(object : MouseMotionAdapter() {
                    override fun mouseDragged(e: MouseEvent) { dragX.set(e.x) }
                })
                panel.addMouseWheelListener { rotation.set(it.wheelRotation) }
            }
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            held = true
            for (step in 1..9) robot.mouseMove(origin.x + 4 + (9 - step) * cellWidth, y)
            ui.waitFor("the physical drag to reach the start of the row") { dragX.get() <= 4 }
            robot.mouseWheel(-4)
            robot.waitForIdle()
            if (rotation.get() > 0 && onEdt { scroll.value == 0 }) robot.mouseWheel(4)
            ui.waitFor("history to scroll while the button stays held") { onEdt { scroll.value < 0 } }
            val endRecord = onEdt { recordAt(buffer, scroll.value + row) }
            assertTrue(endRecord < startRecord)
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            held = false
            robot.waitForIdle()
            val focusOwner = onEdt { KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner }
            assertTrue(onEdt { panel.isFocusOwner }, "Drag left keyboard focus on ${focusOwner?.javaClass?.simpleName}")
            assertTrue(onEdt { panel.javaClass.getMethod("getSelection").invoke(panel) != null }, "The native drag lost its selection")
            val expected = (endRecord..startRecord).joinToString("\n") { "record-%02d".format(it) }
            shortcut(robot, KeyEvent.VK_C)
            ui.waitFor("native Command+C to update the system clipboard") { clipboard.getData(DataFlavor.stringFlavor) == expected }
            assertEquals(expected, clipboard.getData(DataFlavor.stringFlavor), "The full Workbench must copy the highlighted history")
            assertEquals(before, input.readText(), "Dragging and copying must not type or click into the process")
            shortcut(robot, KeyEvent.VK_V)
            val pasted = before + "\u001B[200~" + expected.replace('\n', '\r') + "\u001B[201~"
            ui.waitFor("the real PTY to receive the entire paste") { input.readText() == pasted }
            robot.keyPress(KeyEvent.VK_SHIFT)
            robot.keyPress(KeyEvent.VK_ENTER)
            robot.keyRelease(KeyEvent.VK_ENTER)
            robot.keyRelease(KeyEvent.VK_SHIFT)
            ui.waitFor("the real PTY to receive modified Enter exactly once") { input.readText() == pasted + "\u001B[13;2u" }
            val beforeNavigation = input.readText()
            shortcut(robot, KeyEvent.VK_COMMA)
            ui.waitFor("Command+comma to open Settings while the terminal owns focus") { ui.exists("Appearance & Fonts") }
            assertEquals(beforeNavigation, input.readText(), "An app shortcut must not type into the terminal")
            shortcut(robot, KeyEvent.VK_1, option = true)
            ui.waitFor("Option+Command+1 to return to Work") { !ui.exists("Appearance & Fonts") && ui.exists("Input terminal") }
            ui.waitFor("keyboard focus to return to the terminal") { onEdt { panel.isFocusOwner } }
            val sessionCount = sessions.listSessions().size
            shortcut(robot, KeyEvent.VK_T)
            ui.waitFor("Command+T to open the session chooser") { ui.exists("What do you want to start with?") }
            ui.click("Terminal")
            ui.waitFor("Command+T to open a new shell session") { sessions.listSessions().size > sessionCount }
            Thread.sleep(250)
            assertEquals(sessionCount + 1, sessions.listSessions().size, "One shortcut must open exactly one session")
        } finally {
            if (held) robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            if (previous != null) clipboard.setContents(previous, null)
        }
    }

    private fun withWorkbench(block: (NativeWorkbenchTestWindow, File, InMemoryTerminalSessionManager) -> Unit) {
        val directory = Files.createTempDirectory("moru-workbench-input").toFile()
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        val sessions = InMemoryTerminalSessionManager()
        try {
            val repository = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis)
            val project = repository.registerProject("", directory.path).project
            val task = repository.createTask(project.id, "Input check", "")
            repository.updateSelection(project.id, task.id)
            val input = directory.resolve("input.bin")
            val startup = "stty raw -echo; printf '\\033[?2004h\\033[?1002h'; " +
                "i=1; while [ \$i -le 80 ]; do printf 'record-%02d\\r\\n' \"\$i\"; i=\$((i+1)); done; cat > '${input.path}'"
            sessions.createSession(TerminalSessionSpec(title = "Input terminal", shell = "/bin/sh",
                workingDirectory = directory.path, attachedItemId = task.id, startupCommand = startup))
            val settings = object : SettingsRepository {
                private var value = AppSettings(terminal = TerminalSettings(shellCommand = "/bin/sh"))
                override fun loadSettings() = value
                override fun saveSettings(settings: AppSettings) { value = settings }
            }
            val workspace = object : TerminalWorkspaceRepository {
                override fun loadWorkspaceState() = TerminalWorkspaceState()
                override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
            }
            val commands = MoruCommands()
            NativeWorkbenchTestWindow(1100, 760, menu = { MoruCommandMenuBar(EnglishAppStrings, commands) }) {
                MoruApp(directory.path, sessions, StaticRuntimeStatusRepository, settings, workspace, repository, commands = commands)
            }.use { block(it, input, sessions) }
        } finally {
            sessions.listSessions().forEach { releaseTerminalSession(it.id) }
            database.close()
            directory.deleteRecursively()
        }
    }

    private fun descendants(component: Component): List<Component> = listOf(component) +
        (component as? Container)?.components.orEmpty().flatMap(::descendants)

    private fun focusTerminal(ui: NativeWorkbenchTestWindow, input: File, robot: Robot): Component {
        var terminal: Component? = null
        ui.waitFor("the actual Workbench terminal") {
            terminal = onEdt { Window.getWindows().filter { it.isShowing }.flatMap(::descendants)
                .firstOrNull { it.javaClass.simpleName == "MoruTerminalPanel" && it.isShowing } }
            terminal != null && input.exists()
        }
        val panel = requireNotNull(terminal)
        val scroll = panel.javaClass.getMethod("getVerticalScrollModel").invoke(panel) as BoundedRangeModel
        ui.waitFor("history output") { onEdt { scroll.minimum < 0 } }
        Desktop.getDesktop().requestForeground(true)
        onEdt { SwingUtilities.getWindowAncestor(panel).apply { toFront(); requestFocus() } }
        val origin = onEdt { panel.locationOnScreen }
        robot.mouseMove(origin.x + 20, origin.y + 20)
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        robot.waitForIdle()
        ui.waitFor("native terminal focus and click delivery") { onEdt { panel.isFocusOwner } && input.length() > 0 }
        return panel
    }

    private fun number(target: Any, getter: String): Int = target.javaClass.getMethod(getter).invoke(target) as Int

    private fun lineText(buffer: Any, row: Int): String {
        val line = buffer.javaClass.getMethod("getLine", Int::class.javaPrimitiveType).invoke(buffer, row)
        return line.javaClass.getMethod("getText").invoke(line) as String
    }

    private fun recordAt(buffer: Any, row: Int): Int = lineText(buffer, row).trim().removePrefix("record-").toInt()

    private fun shortcut(robot: Robot, key: Int, option: Boolean = false) {
        robot.keyPress(KeyEvent.VK_META)
        if (option) robot.keyPress(KeyEvent.VK_ALT)
        robot.keyPress(key)
        robot.keyRelease(key)
        if (option) robot.keyRelease(KeyEvent.VK_ALT)
        robot.keyRelease(KeyEvent.VK_META)
        robot.waitForIdle()
    }

    private fun <T> onEdt(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        SwingUtilities.invokeAndWait { result.set(runCatching(block)) }
        return result.get().getOrThrow()
    }
}
