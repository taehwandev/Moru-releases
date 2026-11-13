package app.thdev.moru.app

import app.thdev.moru.app.i18n.EnglishAppStrings
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppShortcutDispatcherTest {
    private val terminal = JPanel()

    @Test
    fun appCommandRunsOnceAndConsumesItsTypedAndReleasedEvents() {
        var opened = 0
        val commands = MoruCommands().apply { showSettings = { opened++ } }
        val dispatcher = dispatcher(commands)
        assertTrue(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA)))
        assertTrue(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA)))
        assertTrue(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_UNDEFINED, id = KeyEvent.KEY_TYPED, char = ',')))
        assertTrue(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA, id = KeyEvent.KEY_RELEASED)))
        assertEquals(1, opened)
    }

    @Test
    fun clipboardTerminalControlsAndExtraModifiersKeepTheirExistingHandlers() {
        val commands = MoruCommands().apply { showSettings = { error("Unexpected app action") } }
        val dispatcher = dispatcher(commands)
        listOf(
            key(KeyEvent.VK_A), key(KeyEvent.VK_C), key(KeyEvent.VK_V),
            key(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK),
            key(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK),
            key(KeyEvent.VK_COMMA, InputEvent.META_DOWN_MASK or InputEvent.ALT_DOWN_MASK),
            key(KeyEvent.VK_COMMA, InputEvent.META_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK),
        ).forEach { event ->
            assertFalse(dispatcher.dispatchKeyEvent(event))
            assertFalse(event.isConsumed)
        }
    }

    @Test
    fun currentEnabledCommandsApplyOnlyInsideTheirOwnWindow() {
        var opened = 0
        val commands = MoruCommands()
        val dispatcher = dispatcher(commands)
        assertFalse(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA)))
        commands.showSettings = { opened++ }
        assertFalse(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA, source = JPanel())))
        assertTrue(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA)))
        dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA, id = KeyEvent.KEY_RELEASED))
        commands.showSettings = null
        assertFalse(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA)))
        assertEquals(1, opened)
    }

    @Test
    fun shiftedCloseProjectAndPlainCloseTerminalStayDistinct() {
        var project = 0
        var terminal = 0
        val commands = MoruCommands().apply {
            closeProject = { project++ }
            closeTerminal = { terminal++ }
        }
        val dispatcher = dispatcher(commands)
        assertTrue(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_W, InputEvent.META_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)))
        dispatcher.dispatchKeyEvent(key(KeyEvent.VK_W, id = KeyEvent.KEY_RELEASED))
        assertTrue(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_W)))
        assertEquals(1, project)
        assertEquals(1, terminal)
    }

    @Test
    fun digitsOpenProjectsAndOptionDigitsOpenTheViews() {
        val ran = mutableListOf<String>()
        val commands = MoruCommands().apply {
            showWork = { ran += "work" }
            projects = listOf(ProjectCommand("first") { ran += "first" })
            nextProject = { ran += "next" }
            nextTask = { ran += "next task" }
            previousSession = { ran += "previous session" }
        }
        val dispatcher = dispatcher(commands)
        listOf(
            key(KeyEvent.VK_1),
            key(KeyEvent.VK_1, InputEvent.META_DOWN_MASK or InputEvent.ALT_DOWN_MASK),
            key(KeyEvent.VK_CLOSE_BRACKET, InputEvent.META_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK),
            key(KeyEvent.VK_CLOSE_BRACKET),
            key(KeyEvent.VK_OPEN_BRACKET, InputEvent.META_DOWN_MASK or InputEvent.ALT_DOWN_MASK),
        ).forEach { event ->
            assertTrue(dispatcher.dispatchKeyEvent(event))
            dispatcher.dispatchKeyEvent(key(event.keyCode, id = KeyEvent.KEY_RELEASED))
        }
        assertFalse(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_2)))
        assertEquals(listOf("first", "work", "next", "next task", "previous session"), ran)
    }

    @Test
    fun nonMacPlatformsUseControlForTheSameAppCommand() {
        var opened = 0
        val commands = MoruCommands().apply { showSettings = { opened++ } }
        val dispatcher = dispatcher(commands, macOs = false)
        assertFalse(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA)))
        assertTrue(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA, InputEvent.CTRL_DOWN_MASK)))
        assertEquals(1, opened)
    }

    @Test
    fun losingWindowFocusDoesNotLeaveOrdinaryTypingSuppressed() {
        val commands = MoruCommands().apply { showSettings = {} }
        val dispatcher = dispatcher(commands)
        assertTrue(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA)))
        dispatcher.reset()
        assertFalse(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_UNDEFINED, 0, KeyEvent.KEY_TYPED, ',')))
        assertFalse(dispatcher.dispatchKeyEvent(key(KeyEvent.VK_COMMA, 0)))
    }

    private fun dispatcher(commands: MoruCommands, macOs: Boolean = true) =
        AppShortcutDispatcher({ appMenu(EnglishAppStrings, commands).flatMap { it.entries } }, macOs) { it === terminal }

    private fun key(
        code: Int,
        modifiers: Int = InputEvent.META_DOWN_MASK,
        id: Int = KeyEvent.KEY_PRESSED,
        char: Char = KeyEvent.CHAR_UNDEFINED,
        source: Component = terminal,
    ) = KeyEvent(source, id, System.currentTimeMillis(), modifiers, code, char)
}
