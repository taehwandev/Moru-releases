package app.thdev.moru.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemePersistenceTest {
    @Test
    fun allThemeIdsRemainStableAndRoundTripWithoutASettingsMigration() {
        val ids = mapOf(
            AppTheme.System to "system", AppTheme.Light to "light", AppTheme.Dark to "dark",
            AppTheme.FreshMint to "fresh_mint", AppTheme.GitHubLight to "github_light",
            AppTheme.GitHubDark to "github_dark", AppTheme.CatppuccinLatte to "catppuccin_latte",
            AppTheme.CatppuccinMocha to "catppuccin_mocha", AppTheme.TokyoNight to "tokyo_night",
        )
        assertEquals(AppTheme.entries.toSet(), ids.keys)
        ids.forEach { (theme, id) ->
            assertEquals("\"$id\"", Json.encodeToString(theme))
            val settings = Json.decodeFromString<AppSettings>("""{"appearance":{"theme":"$id"}}""")
            assertEquals(theme, settings.appearance.theme)
            assertEquals(settings, Json.decodeFromString<AppSettings>(Json.encodeToString(settings)))
        }
    }
}
