package app.thdev.moru.feature.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.model.ShortcutChord
import app.thdev.moru.core.ui.shortcut.ShortcutKeys
import app.thdev.moru.core.ui.shortcut.label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsShortcutsInteractionTest {
    private val platformKey = if (ShortcutKeys.macOs) Key.MetaLeft else Key.CtrlLeft

    @Test
    fun aShortcutTakesTheNextChordAndOneHeldElsewhereMovesWithANote() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        val recording = mutableListOf<Boolean>()
        setContent { MoruTheme { SettingsScreen(EmptyRuntimeStatus, repository, page = "Shortcuts", onRecordingShortcut = { recording += it }) } }
        val settingsField = onNodeWithTag("settings-shortcut-show_settings")
        settingsField.performScrollTo().assertTextEquals(ShortcutChord(",").label())

        settingsField.performScrollTo().performClick()
        settingsField.assertTextEquals("Press keys…")
        runOnIdle { assertTrue(recording.last()) }
        settingsField.performKeyInput { pressKey(Key.J) }
        settingsField.assertTextEquals("Press keys…")
        settingsField.performKeyInput { keyDown(platformKey); keyDown(Key.ShiftLeft); pressKey(Key.J); keyUp(Key.ShiftLeft); keyUp(platformKey) }
        waitUntil(timeoutMillis = 10_000) {
            repository.stored.keyboard.chordFor(AppShortcut.ShowSettings) == ShortcutChord("J", shift = true)
        }
        settingsField.assertTextEquals(ShortcutChord("J", shift = true).label())
        runOnIdle { assertFalse(recording.last()) }

        settingsField.performScrollTo().performClick()
        settingsField.performKeyInput { keyDown(platformKey); pressKey(Key.C); keyUp(platformKey) }
        settingsField.assertTextEquals(ShortcutChord("C").label() + " is kept for copy, paste or the system")
        runOnIdle { assertEquals(ShortcutChord("J", shift = true), repository.stored.keyboard.chordFor(AppShortcut.ShowSettings)) }
        settingsField.performKeyInput { keyDown(platformKey); pressKey(Key.N); keyUp(platformKey) }
        waitUntil(timeoutMillis = 10_000) { repository.stored.keyboard.chordFor(AppShortcut.ShowSettings) == ShortcutChord("N") }
        runOnIdle { assertNull(repository.stored.keyboard.chordFor(AppShortcut.NewTask)) }
        onNodeWithTag("settings-shortcut-moved").assertTextContains("Moved from “New Task” to “Show Settings”.")
        onNodeWithTag("settings-shortcut-new_task").assertTextEquals("None")
        captureSettings("settings-shortcuts")

        settingsField.performScrollTo().performClick()
        settingsField.performKeyInput { pressKey(Key.Escape) }
        settingsField.assertTextEquals(ShortcutChord("N").label())

        settingsField.performScrollTo().performClick()
        settingsField.assertTextEquals("Press keys…")
        settingsField.performKeyInput { pressKey(Key.Backspace) }
        waitUntil(timeoutMillis = 10_000) { repository.stored.keyboard.chordFor(AppShortcut.ShowSettings) == null }
        settingsField.assertTextEquals("None")
    }

    @Test
    fun theDefaultButtonPutsAShortcutBackAndTakesItsDefaultFromWhoeverHoldsIt() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository().apply {
            stored = stored.copy(keyboard = stored.keyboard.assign(AppShortcut.NewTask, ShortcutChord(",")))
        }
        setContent { MoruTheme { SettingsScreen(EmptyRuntimeStatus, repository, page = "Shortcuts") } }
        onNodeWithTag("settings-shortcut-show_settings").performScrollTo().assertTextEquals("None")
        onNodeWithTag("settings-shortcut-reset-show_settings").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.keyboard.chordFor(AppShortcut.ShowSettings) == ShortcutChord(",") }
        onNodeWithTag("settings-shortcut-moved").assertTextContains("Moved from “New Task” to “Show Settings”.")
        onNodeWithTag("settings-shortcut-new_task").assertTextEquals("None")
    }

    @Test
    fun thePageListShowsOnePageAtATimeAndSaysWhichWasOpened() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val opened = mutableListOf<String>()
        setContent { MoruTheme { SettingsScreen(EmptyRuntimeStatus, UiSettingsRepository(), page = "Gone", onPageChange = { opened += it }) } }
        onNodeWithTag("settings-remember-state").assertExists()
        onNodeWithTag("settings-shortcut-show_settings").assertDoesNotExist()
        openSettingsPage(SettingsPage.Shortcuts)
        onNodeWithTag("settings-remember-state").assertDoesNotExist()
        onNodeWithTag("settings-shortcut-show_settings").assertExists()
        runOnIdle { assertEquals(listOf("Shortcuts"), opened) }
    }
}
