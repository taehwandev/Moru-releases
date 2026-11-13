package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.data.DesktopProjectRootCanonicalizer
import app.thdev.moru.core.data.RoomProjectCatalogRepository
import app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.domain.TerminalWorkspaceRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.AppearanceSettings
import app.thdev.moru.core.model.TerminalWorkspaceState
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

/** The welcome opens for a new user only, once, and finishing it shows the workbench. */
@OptIn(ExperimentalTestApi::class)
class OnboardingAppInteractionTest {
    private class Settings : SettingsRepository {
        @Volatile var value = AppSettings(appearance = AppearanceSettings(theme = AppTheme.Light))
        override fun loadSettings() = value
        override fun saveSettings(settings: AppSettings) { value = settings }
    }

    private val workspace = object : TerminalWorkspaceRepository {
        override fun loadWorkspaceState() = TerminalWorkspaceState()
        override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
    }

    private fun withCatalog(block: (RoomProjectCatalogRepository, String) -> Unit) {
        val directory = Files.createTempDirectory("moru-onboarding-ui").toFile()
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            block(
                RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(), { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis),
                directory.resolve("project").apply { mkdir() }.path,
            )
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun aNewUserSeesTheWelcomeOnceAndSkippingItShowsTheWorkbench() = withCatalog { catalog, root ->
        runDesktopComposeUiTest(width = 1100, height = 800) {
            val settings = Settings()
            var launches by mutableStateOf(0)
            setContent {
                key(launches) {
                    MoruApp(root, InMemoryTerminalSessionManager(), StaticRuntimeStatusRepository, settings, workspace, catalog, firstLaunch = true)
                }
            }
            onNodeWithTag("onboarding").assertExists()
            onNodeWithTag("onboarding-skip").performClick()
            waitUntil(timeoutMillis = 10_000) { settings.value.general.onboardingDone }
            onNodeWithTag("onboarding").assertDoesNotExist()

            // Opened again (the app restarted with the same settings), it does not come back.
            launches++
            waitForIdle()
            onNodeWithTag("onboarding").assertDoesNotExist()
            assertTrue(settings.value.general.onboardingDone)
        }
    }

    @Test
    fun someoneWhoAlreadyUsedMoruNeverSeesIt() = withCatalog { catalog, root ->
        runDesktopComposeUiTest(width = 1100, height = 800) {
            setContent {
                MoruApp(root, InMemoryTerminalSessionManager(), StaticRuntimeStatusRepository, Settings(), workspace, catalog, firstLaunch = false)
            }
            waitForIdle()
            onNodeWithTag("onboarding").assertDoesNotExist()
        }
    }
}
