package app.thdev.moru.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.AppearanceSettings
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.feature.settings.SettingsScreen
import java.awt.GraphicsEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class BuiltInThemeSettingsOsTest {
    @Test
    fun everyExplicitThemeCanBeSelectedSavedAndAppliedInTheNativeSettingsWindow() {
        check(!GraphicsEnvironment.isHeadless()) { "Theme visual verification requires a desktop display" }
        val repository = ThemeSettingsRepository()
        var applied by mutableStateOf(repository.stored)
        NativeWorkbenchTestWindow(1200, 880) {
            MoruTheme(colors = applied.appearance.theme.resolveColors(systemDark = false)) {
                CompositionLocalProvider(LocalUiLanguage provides UiLanguage.English) {
                    SettingsScreen(StaticRuntimeStatusRepository, repository, page = "Appearance",
                        onSettingsSaved = { applied = it })
                }
            }
        }.use { window ->
            val choices = mapOf(
                AppTheme.Light to "Light", AppTheme.Dark to "Dark", AppTheme.FreshMint to "Fresh Mint",
                AppTheme.GitHubLight to "GitHub Light", AppTheme.GitHubDark to "GitHub Dark",
                AppTheme.CatppuccinLatte to "Catppuccin Latte", AppTheme.CatppuccinMocha to "Catppuccin Mocha",
                AppTheme.TokyoNight to "Tokyo Night",
            )
            assertEquals(AppTheme.entries.filter { it != AppTheme.System }.toSet(), choices.keys)
            choices.forEach { (theme, label) ->
                window.click(label)
                window.waitFor("$label saved and applied") {
                    repository.stored.appearance.theme == theme && applied.appearance.theme == theme
                }
                assertEquals(theme, repository.loadSettings().appearance.theme)
                window.capture("theme-${theme.name.lowercase()}")
            }
        }
    }

    @Test
    fun explicitPalettesIgnoreSystemAppearanceWhileSystemFollowsIt() {
        AppTheme.entries.filter { it != AppTheme.System }.forEach { theme ->
            assertEquals(theme.resolveColors(false), theme.resolveColors(true), theme.name)
        }
        assertEquals(AppTheme.Light.resolveColors(false), AppTheme.System.resolveColors(false))
        assertEquals(AppTheme.Dark.resolveColors(true), AppTheme.System.resolveColors(true))
        assertEquals(false, AppTheme.GitHubDark.resolveColors(false).isLight)
        assertEquals(false, AppTheme.CatppuccinMocha.resolveColors(false).isLight)
    }
}

private class ThemeSettingsRepository : SettingsRepository {
    @Volatile var stored = AppSettings(appearance = AppearanceSettings(theme = AppTheme.Light))
    override fun loadSettings() = stored
    override fun saveSettings(settings: AppSettings) { stored = settings }
}
