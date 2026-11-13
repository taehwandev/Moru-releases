package app.thdev.moru.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import app.thdev.moru.core.data.DesktopProjectRootCanonicalizer
import app.thdev.moru.core.data.RoomProjectCatalogRepository
import app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.domain.TerminalWorkspaceRepository
import app.thdev.moru.core.model.AppLanguage
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.AppearanceSettings
import app.thdev.moru.core.model.TerminalWorkspaceState
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/** Runs the whole app, so the language setting is checked from the saved choice down to the sidebar text. */
@OptIn(ExperimentalTestApi::class)
class AppLanguageInteractionTest {
    private fun settingsWith(language: AppLanguage) = object : SettingsRepository {
        private var value = AppSettings(appearance = AppearanceSettings(theme = AppTheme.Light, language = language))
        override fun loadSettings() = value
        override fun saveSettings(settings: AppSettings) { value = settings }
    }

    private fun workspace() = object : TerminalWorkspaceRepository {
        override fun loadWorkspaceState() = TerminalWorkspaceState()
        override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
    }

    /** Opening a project lives in the project switcher's menu, so open it, read the entry and close it again. */
    private fun SemanticsNodeInteractionsProvider.assertProjectMenuOffers(openProject: String, instead: String? = null) {
        onNodeWithContentDescription("Switch project").performClick()
        onNodeWithText(openProject).assertExists()
        instead?.let { onNodeWithText(it).assertDoesNotExist() }
        onNodeWithContentDescription("Switch project").performClick()
    }

    private fun withCatalog(block: (RoomProjectCatalogRepository, File) -> Unit) {
        val directory = Files.createTempDirectory("moru-language-ui").toFile()
        val root = directory.resolve("Example project").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            block(
                RoomProjectCatalogRepository(
                    database, DesktopProjectRootCanonicalizer(), { java.util.UUID.randomUUID().toString() },
                    System::currentTimeMillis,
                ),
                root,
            )
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun aKoreanChoiceShowsKoreanEvenOnAnEnglishComputer() = withCatalog { catalog, root ->
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruApp(
                    root.path, InMemoryTerminalSessionManager(), StaticRuntimeStatusRepository,
                    settingsWith(AppLanguage.Korean), workspace(), catalog, systemLanguageTag = "en-US",
                )
            }
            assertProjectMenuOffers("프로젝트 열기…", instead = "Open project…")
            // The app shell around the sidebar follows the same language.
            onNodeWithText("다음 작업을 위한 공간").assertExists()
            onNodeWithContentDescription("설정").assertExists()
            onNodeWithText("A place for your next task").assertDoesNotExist()
        }
    }

    @Test
    fun followingTheSystemGivesKoreanOnAKoreanComputerAndEnglishOtherwise() = withCatalog { catalog, root ->
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruApp(
                    root.path, InMemoryTerminalSessionManager(), StaticRuntimeStatusRepository,
                    settingsWith(AppLanguage.System), workspace(), catalog, systemLanguageTag = "ko-KR",
                )
            }
            assertProjectMenuOffers("프로젝트 열기…")
        }
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruApp(
                    root.path, InMemoryTerminalSessionManager(), StaticRuntimeStatusRepository,
                    settingsWith(AppLanguage.System), workspace(), catalog, systemLanguageTag = "ja-JP",
                )
            }
            assertProjectMenuOffers("Open project…")
        }
    }

    @Test
    fun savingAnotherLanguageRedrawsTheRunningApp() = withCatalog { catalog, root ->
        val repository = settingsWith(AppLanguage.English)
        val state = MoruAppState(repository)
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruApp(
                    root.path, InMemoryTerminalSessionManager(), StaticRuntimeStatusRepository,
                    repository, workspace(), catalog, appState = state, systemLanguageTag = "en-US",
                )
            }
            assertProjectMenuOffers("Open project…")

            runOnIdle { state.settingsSaved(AppSettings(appearance = AppearanceSettings(theme = AppTheme.Light, language = AppLanguage.Korean))) }

            assertProjectMenuOffers("프로젝트 열기…", instead = "Open project…")
        }
    }

    @Test
    fun changingAppearanceSavesAndAppliesWithoutAButtonAndSurvivesLeavingSettings() = withCatalog { catalog, root ->
        val repository = settingsWith(AppLanguage.English)
        val state = MoruAppState(repository)
        runDesktopComposeUiTest(width = 1400, height = 900) {
            setContent {
                MoruApp(
                    root.path, InMemoryTerminalSessionManager(), StaticRuntimeStatusRepository,
                    repository, workspace(), catalog, appState = state, systemLanguageTag = "en-US",
                )
            }
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithTag("settings-page-Appearance").performClick()
            onNodeWithText("Save Settings").assertDoesNotExist()
            onNodeWithText("Dark").performScrollTo().performClick()
            onNodeWithContentDescription("Workbench").performClick()
            waitUntil(timeoutMillis = 10_000) { state.settings.appearance.theme == AppTheme.Dark }
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithTag("settings-page-Appearance").performClick()
            onNodeWithText("한국어").performScrollTo().performClick()
            waitUntil(timeoutMillis = 10_000) { state.settings.appearance.language == AppLanguage.Korean }
            onNodeWithContentDescription("설정").assertExists()
            runOnIdle {
                assertEquals(AppTheme.Dark, repository.loadSettings().appearance.theme)
                assertEquals(AppLanguage.Korean, repository.loadSettings().appearance.language)
            }
        }
    }
}
