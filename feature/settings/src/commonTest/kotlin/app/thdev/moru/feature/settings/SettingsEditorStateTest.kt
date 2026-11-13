package app.thdev.moru.feature.settings

import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppLanguage
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.WindowSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsEditorStateTest {
    @Test
    fun openingTheEditorDoesNotWriteSettings() = runTest {
        val repository = EditorTestRepository()
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler)) {}
        runCurrent()
        assertEquals(0, repository.saves.size)
        assertEquals(SettingsSaveStatus.Ready, state.saveStatus)
        state.change { it }
        runCurrent()
        assertEquals(0, repository.saves.size)
    }

    @Test
    fun rapidEditsPersistTheLatestChoiceAndReportSuccessOnlyAfterWriting() = runTest {
        val repository = EditorTestRepository()
        val applied = mutableListOf<AppSettings>()
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler), applied::add)
        state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.Dark)) }
        state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.Light)) }
        state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.FreshMint)) }
        assertEquals(SettingsSaveStatus.Saving, state.saveStatus)
        assertTrue(applied.isEmpty())
        runCurrent()
        assertEquals(AppTheme.FreshMint, repository.stored.appearance.theme)
        assertEquals(1, repository.saves.size)
        assertEquals(listOf(repository.stored), applied)
        assertEquals(SettingsSaveStatus.Saved, state.saveStatus)
    }

    @Test
    fun aNewEditDuringWritingCannotApplyAnOlderThemeOrReportSaved() = runTest {
        val repository = EditorTestRepository()
        val applied = mutableListOf<AppSettings>()
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler), applied::add)
        repository.beforeFirstSave = {
            state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.TokyoNight)) }
            assertEquals(SettingsSaveStatus.Saving, state.saveStatus)
            assertTrue(applied.isEmpty())
        }
        state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.Dark)) }
        runCurrent()
        assertEquals(listOf(AppTheme.Dark, AppTheme.TokyoNight), repository.saves.map { it.appearance.theme })
        assertEquals(listOf(AppTheme.TokyoNight), applied.map { it.appearance.theme })
        assertEquals(SettingsSaveStatus.Saved, state.saveStatus)
    }

    @Test
    fun anExternalWindowUpdateAndSchemaArePreservedByAnAppearanceSave() = runTest {
        val repository = EditorTestRepository()
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler)) {}
        val latestWindow = WindowSettings(widthDp = 1870, heightDp = 990, xDp = 42, maximized = true)
        repository.stored = repository.stored.copy(window = latestWindow, schemaVersion = 2)
        state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.Dark)) }
        runCurrent()
        assertEquals(latestWindow, repository.stored.window)
        assertEquals(2, repository.stored.schemaVersion)
        assertEquals(repository.stored, state.settings)
    }

    @Test
    fun failedWritesKeepTheDraftAndRetryWithoutClaimingSuccess() = runTest {
        val repository = EditorTestRepository().apply { failuresRemaining = 1 }
        val applied = mutableListOf<AppSettings>()
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler), applied::add)
        state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.Dark)) }
        runCurrent()
        assertEquals(SettingsSaveStatus.Failed, state.saveStatus)
        assertEquals("Disk unavailable", state.saveError)
        assertEquals(AppTheme.Dark, state.settings.appearance.theme)
        assertEquals(AppTheme.System, repository.stored.appearance.theme)
        assertTrue(applied.isEmpty())
        state.retry()
        assertEquals(SettingsSaveStatus.Saving, state.saveStatus)
        runCurrent()
        assertEquals(AppTheme.Dark, repository.stored.appearance.theme)
        assertEquals(SettingsSaveStatus.Saved, state.saveStatus)
        assertEquals(listOf(repository.stored), applied)
    }

    @Test
    fun invalidAndOutOfRangeNumbersRemainEditableAndAreNeverClamped() = runTest {
        val repository = EditorTestRepository()
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler)) {}
        listOf("", "abc", "15px", "0", "29", "9999999999999999999999").forEach {
            state.changeFontSize(it)
            runCurrent()
            assertEquals(it, state.fontSizeText)
            assertTrue(state.hasInvalidNumbers)
            assertEquals(13, repository.stored.appearance.terminalFontSize)
        }
        assertTrue(repository.saves.isEmpty())
        state.changeFontSize("18")
        runCurrent()
        assertFalse(state.hasInvalidNumbers)
        assertEquals(18, repository.stored.appearance.terminalFontSize)
    }

    @Test
    fun invalidScrollbackDoesNotPreventOtherValidChangesFromBeingSaved() = runTest {
        val repository = EditorTestRepository()
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler)) {}
        state.changeScrollback("49")
        state.change { it.copy(general = it.general.copy(rememberLastState = false)) }
        runCurrent()
        assertFalse(repository.stored.general.rememberLastState)
        assertEquals(5000, repository.stored.terminal.scrollbackLines)
        assertTrue(state.hasInvalidNumbers)
        state.changeScrollback("20000")
        runCurrent()
        assertEquals(20000, repository.stored.terminal.scrollbackLines)
        assertFalse(state.hasInvalidNumbers)
    }

    @Test
    fun aStaleEditorOnlyWritesTheFieldTheUserChanged() = runTest {
        val repository = EditorTestRepository()
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler)) {}
        repository.stored = repository.stored.copy(
            appearance = repository.stored.appearance.copy(language = AppLanguage.Korean, terminalFontSize = 20),
            terminal = repository.stored.terminal.copy(shellCommand = "/bin/fish"),
        )
        state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.Dark)) }
        runCurrent()
        assertEquals(AppTheme.Dark, repository.stored.appearance.theme)
        assertEquals(AppLanguage.Korean, repository.stored.appearance.language)
        assertEquals(20, repository.stored.appearance.terminalFontSize)
        assertEquals("/bin/fish", repository.stored.terminal.shellCommand)
        assertEquals(repository.stored, state.settings)
    }

    @Test
    fun reopeningSettingsClearsAnEarlierSaveConfirmation() = runTest {
        val repository = EditorTestRepository()
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler)) {}
        state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.Dark)) }
        runCurrent()
        assertEquals(SettingsSaveStatus.Saved, state.saveStatus)
        state.refresh()
        assertEquals(SettingsSaveStatus.Ready, state.saveStatus)
        assertEquals(AppTheme.Dark, state.settings.appearance.theme)
    }

    @Test
    fun reopeningSettingsDropsANeverValidDraftButKeepsUnsavedEdits() = runTest {
        val repository = EditorTestRepository().apply { failuresRemaining = 1 }
        val state = SettingsEditorState(repository, backgroundScope, StandardTestDispatcher(testScheduler)) {}
        state.changeFontSize("")
        state.refresh()
        assertEquals("13", state.fontSizeText)
        assertFalse(state.hasInvalidNumbers)
        state.change { it.copy(appearance = it.appearance.copy(theme = AppTheme.Dark)) }
        runCurrent()
        assertEquals(SettingsSaveStatus.Failed, state.saveStatus)
        state.refresh()
        assertEquals(AppTheme.Dark, state.settings.appearance.theme)
        assertEquals(SettingsSaveStatus.Failed, state.saveStatus)
    }
}

private class EditorTestRepository : SettingsRepository {
    var stored = AppSettings()
    val saves = mutableListOf<AppSettings>()
    var failuresRemaining = 0
    var beforeFirstSave: (() -> Unit)? = null
    override fun loadSettings() = stored
    override fun saveSettings(settings: AppSettings) {
        beforeFirstSave?.also { beforeFirstSave = null }?.invoke()
        if (failuresRemaining > 0) {
            failuresRemaining -= 1
            error("Disk unavailable")
        }
        stored = settings
        saves += settings
    }
}
