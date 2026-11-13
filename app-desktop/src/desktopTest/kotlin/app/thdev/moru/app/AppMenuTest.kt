package app.thdev.moru.app

import androidx.compose.ui.input.key.Key
import app.thdev.moru.app.i18n.EnglishAppStrings
import app.thdev.moru.app.i18n.KoreanAppStrings
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.model.KeyboardSettings
import app.thdev.moru.core.model.ShortcutChord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppMenuTest {
    private fun entries(commands: MoruCommands) =
        appMenu(EnglishAppStrings, commands).flatMap { group -> group.entries }.associateBy { entry -> entry.label }

    @Test
    fun anItemIsEnabledExactlyWhileItsCommandIsRegistered() {
        val commands = MoruCommands()
        assertTrue(entries(commands).values.none { entry -> entry.enabled })

        commands.openProject = {}
        commands.showSettings = {}
        val enabled = entries(commands).values.filter { entry -> entry.enabled }.map { entry -> entry.label }

        assertEquals(listOf("Open Project…", "Settings"), enabled)
    }

    @Test
    fun anItemRunsItsCommandAndADisabledOneRunsNothing() {
        val commands = MoruCommands()
        var opened = 0
        commands.openProject = { opened++ }

        entries(commands).getValue("Open Project…").onClick()
        entries(commands).getValue("New Task").onClick()

        assertEquals(1, opened)
    }

    @Test
    fun theShortcutsAreTheDocumentedOnesAndNoTwoItemsShareOne() {
        val commands = MoruCommands().apply { projects = List(10) { index -> ProjectCommand("Project $index") {} } }
        val all = appMenu(EnglishAppStrings, commands).flatMap { group -> group.entries }

        assertEquals(
            listOf(
                "O", "N", "T", "D", "⇧D", "W", "⇧T", "⇧W", null, "⇧Q",
                "⌥1", "⌥2", "⌥3", "]", "[", "⌥]", "⌥[", "⇧K", ",",
                "⇧]", "⇧[", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            ),
            all.map { entry -> entry.key?.let { (if (entry.alt) "⌥" else "") + (if (entry.shift) "⇧" else "") + KeyNames.getValue(it) } },
        )
        val shortcuts = all.filter { entry -> entry.key != null }.map { entry -> Triple(entry.key, entry.shift, entry.alt) }
        assertEquals(shortcuts.size, shortcuts.toSet().size)
    }

    @Test
    fun theDigitsOpenTheFirstNineProjectsInSidebarOrder() {
        val opened = mutableListOf<Int>()
        val commands = MoruCommands().apply { projects = List(10) { index -> ProjectCommand("Project $index") { opened += index } } }
        val projectItems = appMenu(EnglishAppStrings, commands).last().entries.drop(2)

        assertEquals((0..8).map { index -> "Project $index" }, projectItems.map { entry -> entry.label })
        projectItems.forEach { entry -> entry.onClick() }
        assertEquals((0..8).toList(), opened)
    }

    @Test
    fun theKeyboardSettingsMoveAShortcutAndRecordingOneStandsEveryItemAside() {
        val commands = MoruCommands().apply {
            newTask = {}
            showSettings = {}
            keyboard = KeyboardSettings().assign(AppShortcut.ShowSettings, ShortcutChord("N", alt = true))
        }
        val settings = entries(commands).getValue("Settings")
        assertEquals(Triple(Key.N, false, true), Triple(settings.key, settings.shift, settings.alt))
        assertEquals(Key.N, entries(commands).getValue("New Task").key)

        commands.recordingShortcut = true
        assertTrue(entries(commands).values.none { entry -> entry.enabled })
    }

    @Test
    fun theMenuIsKoreanInKoreanAndEveryLabelIsFilled() {
        val korean = appMenu(KoreanAppStrings, MoruCommands())

        assertEquals(listOf("파일", "보기", "프로젝트"), korean.map { group -> group.label })
        assertFalse(korean.flatMap { group -> group.entries }.any { entry -> entry.label.isBlank() })
    }

    private companion object {
        val KeyNames = mapOf(
            Key.O to "O", Key.N to "N", Key.T to "T", Key.D to "D", Key.W to "W", Key.Q to "Q", Key.K to "K", Key.Comma to ",",
            Key.RightBracket to "]", Key.LeftBracket to "[", Key.One to "1", Key.Two to "2", Key.Three to "3",
            Key.Four to "4", Key.Five to "5", Key.Six to "6", Key.Seven to "7", Key.Eight to "8", Key.Nine to "9",
        )
    }
}
