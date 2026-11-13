package app.thdev.moru.feature.terminal

import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalModifiedEnterTest {
    @Test
    fun shiftEnterSendsOneDistinctModifiedReturnThenNormalEnterStillSendsCarriageReturn() {
        PreeditTestFixture().use { fixture ->
            enter(fixture, InputEvent.SHIFT_DOWN_MASK)
            enter(fixture, 0)

            assertTrue(
                fixture.connector.awaitText("\u001B[13;2u\r"),
                "Shift+Enter must remain a modified key rather than submitting a normal Enter.",
            )
            assertEquals("\u001B[13;2u\r", fixture.connector.writtenText())
        }
    }

    @Test
    fun aTypedReturnWithoutItsShiftModifierDoesNotSendASecondReturn() {
        PreeditTestFixture().use { fixture ->
            enter(fixture, InputEvent.SHIFT_DOWN_MASK, trailingModifiers = 0)
            enter(fixture, InputEvent.SHIFT_DOWN_MASK, trailingModifiers = 0)
            enter(fixture, 0)

            assertTrue(fixture.connector.awaitText("\u001B[13;2u\u001B[13;2u\r"))
            assertEquals("\u001B[13;2u\u001B[13;2u\r", fixture.connector.writtenText())
        }
    }

    @Test
    fun shiftEnterRemainsWithTheActiveCompositionUntilItIsCommitted() {
        PreeditTestFixture().use { fixture ->
            fixture.composition("한")
            fixture.caretChanged(1)
            enter(fixture, InputEvent.SHIFT_DOWN_MASK)

            assertFalse(fixture.connector.hasWriteWithin(150), "Marked text must keep Shift+Enter out of the PTY.")
            fixture.composition("한", committedCharacters = 1)
            enter(fixture, InputEvent.SHIFT_DOWN_MASK)

            assertTrue(fixture.connector.awaitText("한\u001B[13;2u"))
            assertEquals("한\u001B[13;2u", fixture.connector.writtenText())
        }
    }

    @Test
    fun otherModifiersAndKeysRemainAvailableToJediTerm() {
        val sent = mutableListOf<String>()
        val handler = TerminalModifiedEnter(sent::add)
        val source = JPanel()
        val modifiers = listOf(
            0, InputEvent.ALT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.META_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK or InputEvent.ALT_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK or InputEvent.META_DOWN_MASK,
        )
        for (modifier in modifiers) {
            val event = KeyEvent(source, KeyEvent.KEY_PRESSED, 0L, modifier, KeyEvent.VK_ENTER, '\n')
            assertFalse(handler.handleKeyEvent(event), "Unsupported modifier $modifier must retain JediTerm handling.")
            assertFalse(event.isConsumed)
        }
        val shiftedTab = KeyEvent(source, KeyEvent.KEY_PRESSED, 0L, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_TAB, '\t')
        assertFalse(handler.handleKeyEvent(shiftedTab))
        assertFalse(shiftedTab.isConsumed)
        assertEquals(emptyList(), sent)
    }

    @Test
    fun theFollowingKeyAndFocusResetDoNotLeaveAStaleTypedReturnSuppression() {
        val sent = mutableListOf<String>()
        val handler = TerminalModifiedEnter(sent::add)
        val source = JPanel()
        fun key(id: Int, code: Int, character: Char, modifiers: Int = 0) =
            KeyEvent(source, id, 0L, modifiers, code, character)

        assertTrue(handler.handleKeyEvent(key(KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, '\n', InputEvent.SHIFT_DOWN_MASK)))
        assertFalse(handler.handleKeyEvent(key(KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, '\n')))
        assertFalse(handler.handleKeyEvent(key(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '\n')))
        assertTrue(handler.handleKeyEvent(key(KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, '\n', InputEvent.SHIFT_DOWN_MASK)))
        handler.reset()
        assertFalse(handler.handleKeyEvent(key(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '\n')))
        assertEquals(listOf("\u001B[13;2u", "\u001B[13;2u"), sent)
    }

    private fun enter(fixture: PreeditTestFixture, modifiers: Int, trailingModifiers: Int = modifiers) {
        SwingUtilities.invokeAndWait {
            fixture.panel.processKeyEvent(KeyEvent(fixture.panel, KeyEvent.KEY_PRESSED, 0L, modifiers, KeyEvent.VK_ENTER, '\n'))
            fixture.panel.processKeyEvent(KeyEvent(fixture.panel, KeyEvent.KEY_TYPED, 1L, trailingModifiers, KeyEvent.VK_UNDEFINED, '\n'))
            fixture.panel.processKeyEvent(KeyEvent(fixture.panel, KeyEvent.KEY_RELEASED, 2L, trailingModifiers, KeyEvent.VK_ENTER, '\n'))
        }
    }
}
